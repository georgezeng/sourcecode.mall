package com.sourcecode.malls.service.impl;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import com.alibaba.druid.util.StringUtils;
import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.coupon.ClientCoupon;
import com.sourcecode.malls.domain.coupon.CouponSetting;
import com.sourcecode.malls.domain.goods.GoodsCategory;
import com.sourcecode.malls.domain.goods.GoodsItem;
import com.sourcecode.malls.domain.merchant.Merchant;
import com.sourcecode.malls.domain.merchant.MerchantShopApplication;
import com.sourcecode.malls.domain.order.Order;
import com.sourcecode.malls.domain.order.SubOrder;
import com.sourcecode.malls.dto.ClientCouponDTO;
import com.sourcecode.malls.dto.client.ClientDTO;
import com.sourcecode.malls.dto.query.PageInfo;
import com.sourcecode.malls.dto.query.QueryInfo;
import com.sourcecode.malls.enums.CashCouponEventType;
import com.sourcecode.malls.enums.ClientCouponStatus;
import com.sourcecode.malls.enums.CouponSettingStatus;
import com.sourcecode.malls.enums.Sex;
import com.sourcecode.malls.properties.SuperAdminProperties;
import com.sourcecode.malls.repository.jpa.impl.client.ClientRepository;
import com.sourcecode.malls.repository.jpa.impl.coupon.ClientCouponRepository;
import com.sourcecode.malls.repository.jpa.impl.coupon.CouponSettingRepository;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantRepository;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantShopApplicationRepository;
import com.sourcecode.malls.service.FileOnlineSystemService;
import com.sourcecode.malls.service.base.BaseService;
import com.sourcecode.malls.service.base.JpaService;
import com.sourcecode.malls.util.AssertUtil;
import com.sourcecode.malls.util.ImageUtil;

@Service("ClientDetailsService")
@Transactional
public class ClientService implements BaseService, UserDetailsService, JpaService<Client, Long> {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private MerchantRepository merchantRepository;

	@Autowired
	private MerchantShopApplicationRepository merchantShopRepository;

	@Autowired
	private SuperAdminProperties adminProperties;

	@Autowired
	private FileOnlineSystemService fileService;

	@Autowired
	private CouponSettingRepository couponSettingRepository;

	@Autowired
	private ClientCouponRepository clientCouponRepository;

	@Autowired
	private RestTemplate httpClient;

	@Autowired
	private ImageService imageService;

	@Value("${invite.image.background.path}")
	private String shareBgPath;

	@Value("${user.avatar.default.path}")
	private String userAvatarDefaultPath;

	@Value("${user.type.name}")
	private String userDir;

	@Autowired
	private PasswordEncoder pwdEncoder;

	@Autowired
	private EntityManager em;
	
	@Autowired
	private CacheEvictService cacheEvictService;

	public void setConsumeBonus(Order order) {
		if (CollectionUtils.isEmpty(order.getSubList())) {
			return;
		}
		List<CouponSetting> list = couponSettingRepository.findAllByMerchantAndEventTypeAndStatusAndEnabled(
				order.getMerchant(), CashCouponEventType.Consume, CouponSettingStatus.PutAway, true);
		if (!CollectionUtils.isEmpty(list)) {
			for (CouponSetting setting : list) {
				if (setting.getConsumeSetting() != null) {
					BigDecimal upToAmount = BigDecimal.ZERO;
					for (SubOrder sub : order.getSubList()) {
						boolean match = false;
						switch (setting.getConsumeSetting().getType()) {
						case All:
							match = true;
							break;
						case Category: {
							if (!CollectionUtils.isEmpty(setting.getConsumeSetting().getCategories())) {
								for (GoodsCategory category : setting.getConsumeSetting().getCategories()) {
									if (sub.getItem().getCategory().getId().equals(category.getId())) {
										match = true;
										break;
									}
								}
							}
						}
							break;
						case Item: {
							if (!CollectionUtils.isEmpty(setting.getConsumeSetting().getItems())) {
								for (GoodsItem item : setting.getConsumeSetting().getItems()) {
									if (sub.getItem().getId().equals(item.getId())) {
										match = true;
										break;
									}
								}
							}
						}
							break;
						}
						if (match) {
							upToAmount = upToAmount.add(sub.getDealPrice());
						}
					}
					if (upToAmount.compareTo(setting.getConsumeSetting().getUpToAmount()) >= 0) {
						createCoupon(order, order.getClient(), setting, true);
					}
				}
			}
		}
	}

	private void createCoupon(Order order, Client client, CouponSetting setting, boolean require) {
		if (!require) {
			List<ClientCoupon> list = clientCouponRepository.findAllByClientAndSetting(client, setting);
			require = CollectionUtils.isEmpty(list);
		}
		if (require) {
			em.lock(setting, LockModeType.PESSIMISTIC_WRITE);
			if (setting.getTotalNums() == 0 || setting.getSentNums() < setting.getTotalNums()) {
				ClientCoupon coupon = new ClientCoupon();
				coupon.setClient(client);
				coupon.setMerchant(client.getMerchant());
				coupon.setSetting(setting);
				coupon.setCouponId(generateId());
				coupon.setReceivedTime(new Date());
				coupon.setStatus(ClientCouponStatus.UnUse);
				coupon.setFromOrder(order);
				clientCouponRepository.save(coupon);
				setting.setSentNums(setting.getSentNums() + 1);
				if (setting.getSentNums() == setting.getTotalNums()) {
					setting.setStatus(CouponSettingStatus.SentOut);
				}
				couponSettingRepository.save(setting);
				cacheEvictService.clearClientCoupons(client.getId());
			}
		}
	}

	public void setInviteBonus(Long userId) {
		Optional<Client> userOp = clientRepository.findById(userId);
		AssertUtil.assertTrue(userOp.isPresent(), "推荐用户不存在");
		Client user = userOp.get();
		List<CouponSetting> list = couponSettingRepository.findAllByMerchantAndEventTypeAndStatusAndEnabled(
				user.getMerchant(), CashCouponEventType.Invite, CouponSettingStatus.PutAway, true);
		if (!CollectionUtils.isEmpty(list)) {
			for (CouponSetting setting : list) {
				if (setting.getInviteSetting() != null && !CollectionUtils.isEmpty(user.getSubList())) {
					int times = user.getSubList().size() / setting.getInviteSetting().getMemberNums();
					int nums = clientCouponRepository.findAllByClientAndSetting(user, setting).size();
					while (nums < times) {
						createCoupon(null, user, setting, true);
						nums++;
					}
				}
			}
		}
	}

	public void setRegistrationBonus(Long userId) {
		Optional<Client> user = clientRepository.findById(userId);
		AssertUtil.assertTrue(user.isPresent(), "用户不存在");
		List<CouponSetting> list = couponSettingRepository.findAllByMerchantAndEventTypeAndStatusAndEnabled(
				user.get().getMerchant(), CashCouponEventType.Registration, CouponSettingStatus.PutAway, true);
		if (!CollectionUtils.isEmpty(list)) {
			for (CouponSetting setting : list) {
				createCoupon(null, user.get(), setting, false);
			}
		}
	}

	@Cacheable(cacheNames = "client_unuse_coupon_nums", key = "#userId")
	public long countUnUseCouponNums(Long userId) {
		Optional<Client> user = clientRepository.findById(userId);
		AssertUtil.assertTrue(user.isPresent(), "用户不存在");
		Specification<ClientCoupon> spec = new Specification<ClientCoupon>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Predicate toPredicate(Root<ClientCoupon> root, CriteriaQuery<?> query,
					CriteriaBuilder criteriaBuilder) {
				List<Predicate> predicate = new ArrayList<>();
				predicate.add(criteriaBuilder.equal(root.get("client"), user.get()));
				predicate.add(criteriaBuilder.equal(root.get("status"), ClientCouponStatus.UnUse));
				return query.where(predicate.toArray(new Predicate[] {})).getRestriction();
			}
		};
		return clientCouponRepository.count(spec);
	}

	@Transactional(readOnly = true)
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Long merchantId = ClientContext.getMerchantId();
		AssertUtil.assertNotNull(merchantId, "商户不存在");
		Optional<Merchant> merchant = merchantRepository.findById(merchantId);
		if (adminProperties.getUsername().equals(username)) {
			return getAdmin(merchant.get());
		}
		AssertUtil.assertTrue(merchant.isPresent(), "商户不存在");
		Optional<Client> client = clientRepository.findByMerchantAndUsername(merchant.get(), username);
		if (!client.isPresent()) {
			throw new UsernameNotFoundException("用户名或密码有误");
		}
		return client.get();
	}

	@Transactional(readOnly = true)
	public Client getAdmin(Merchant merchant) {
		Client admin = new Client();
		admin.setId(0l);
		admin.setUsername(adminProperties.getUsername());
		admin.setPassword(pwdEncoder.encode(adminProperties.getPassword()));
		admin.setNickname("管理员");
		admin.setSex(Sex.Secret);
		admin.setEnabled(true);
		admin.setMerchant(merchant);
		admin.setAuth(adminProperties.getAuthority());
		return admin;
	}

	@Transactional(readOnly = true)
	public Client findByMerchantAndUsername(Long merchantId, String username) {
		Optional<Merchant> merchant = merchantRepository.findById(merchantId);
		AssertUtil.assertTrue(merchant.isPresent(), "商户不存在");
		if (username.equals(adminProperties.getUsername())) {
			return getAdmin(merchant.get());
		}
		Optional<Client> client = clientRepository.findByMerchantAndUsername(merchant.get(), username);
		AssertUtil.assertTrue(client.isPresent(), "用户不存在");
		client.get().getMerchant();
		return client.get();
	}

	@Transactional(readOnly = true)
	public Optional<Client> findById(Long id) {
		Optional<Client> client = clientRepository.findById(id);
		AssertUtil.assertTrue(client.isPresent(), "用户不存在");
		client.get().getMerchant();
		return client;
	}

	@Override
	public JpaRepository<Client, Long> getRepository() {
		return clientRepository;
	}

	@Cacheable(cacheNames = "client_invite_poster", key = "#userId")
	public byte[] loadInvitePoster(Long userId) throws Exception {
		Optional<Client> clientOp = clientRepository.findById(userId);
		AssertUtil.assertTrue(clientOp.isPresent(), "用户不存在");
		Client client = clientOp.get();
		String nickname = client.getNickname();
		if (StringUtils.isEmpty(nickname)) {
			nickname = "****" + client.getUsername().substring(7);
		}
		Optional<MerchantShopApplication> app = merchantShopRepository.findByMerchantId(client.getMerchant().getId());
		AssertUtil.assertTrue(app.isPresent(), "商铺信息不存在");
		String shareQrCodeUrl = "https://" + app.get().getDomain() + "/?uid=" + userId + "#/Home";
		BufferedImage qrCode = ImageUtil.resizeImage(imageService.generateQRCodeImage(shareQrCodeUrl, 1000, 1000, 1),
				250, 250);
		String avatar = client.getAvatar();
		InputStream in = null;
		if (StringUtils.isEmpty(avatar)) {
			in = new ByteArrayInputStream(fileService.load(true, userAvatarDefaultPath));
		} else if (avatar.startsWith("http")) {
			in = new ByteArrayInputStream(httpClient.getForEntity(avatar, byte[].class).getBody());
		} else {
			in = new ByteArrayInputStream(fileService.load(true, avatar));
		}
		int avatarSize = 160;
		BufferedImage avatarImage = ImageIO.read(in);
		if (avatarImage.getWidth() > avatarImage.getHeight()) {
			avatarImage = ImageUtil.rotateImage(avatarImage, 90);
			avatarImage = ImageUtil.resizeImage(avatarImage, avatarSize, avatarSize);
		}
		BufferedImage result = ImageIO.read(new ByteArrayInputStream(fileService.load(true, shareBgPath)));
		Graphics2D g = (Graphics2D) result.getGraphics();
		g.drawImage(qrCode, 250, 770, null);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.DARK_GRAY);
		ImageUtil.drawCenteredString(g, nickname, 0, 170, result.getWidth(), 45,
				imageService.getFont().deriveFont(30f).deriveFont(Font.BOLD));
		g.setClip(new Ellipse2D.Float(300, 10, avatarSize, avatarSize));
		g.drawImage(avatarImage, 300, 10, avatarSize, avatarSize, null);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(result, "png", out);
		byte[] arr = out.toByteArray();
		return arr;
	}

	@Transactional(readOnly = true)
	public List<ClientDTO> getSubList(Client parent, PageInfo page) {
		List<Client> result = clientRepository.findAllByParent(parent, page.pageable());
		return result.stream().map(it -> {
			ClientDTO dto = it.asDTO();
			if (StringUtils.isEmpty(it.getNickname())) {
				dto.setNickname("未设置昵称");
			}
			dto.setUsername("****" + dto.getUsername().substring(7));
			return dto;
		}).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<ClientCouponDTO> getCoupons(Client client, QueryInfo<ClientCouponStatus> queryInfo) {
		AssertUtil.assertNotNull(queryInfo.getData(), "参数不正确");
		Specification<ClientCoupon> spec = new Specification<ClientCoupon>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Predicate toPredicate(Root<ClientCoupon> root, CriteriaQuery<?> query,
					CriteriaBuilder criteriaBuilder) {
				List<Predicate> predicate = new ArrayList<>();
				predicate.add(criteriaBuilder.equal(root.get("client"), client));
				predicate.add(criteriaBuilder.equal(root.get("status"), queryInfo.getData()));
				return query.where(predicate.toArray(new Predicate[] {})).getRestriction();
			}
		};
		Page<ClientCoupon> pageResult = clientCouponRepository.findAll(spec, queryInfo.getPage().pageable());
		return getCouponList(pageResult.get());
	}

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = "client_coupon_nums", key = "#client.id.toString() + '-' + #queryInfo.data.name()")
	public long countCoupons(Client client, QueryInfo<ClientCouponStatus> queryInfo) {
		AssertUtil.assertNotNull(queryInfo.getData(), "参数不正确");
		Specification<ClientCoupon> spec = new Specification<ClientCoupon>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Predicate toPredicate(Root<ClientCoupon> root, CriteriaQuery<?> query,
					CriteriaBuilder criteriaBuilder) {
				List<Predicate> predicate = new ArrayList<>();
				predicate.add(criteriaBuilder.equal(root.get("client"), client));
				predicate.add(criteriaBuilder.equal(root.get("status"), queryInfo.getData()));
				return query.where(predicate.toArray(new Predicate[] {})).getRestriction();
			}
		};
		return clientCouponRepository.count(spec);
	}

	public List<ClientCouponDTO> getCouponList(Stream<ClientCoupon> stream) {
		return stream.map(coupon -> {
			ClientCouponDTO data = new ClientCouponDTO();
			BeanUtils.copyProperties(coupon.getSetting(), data, "id", "categories", "item");
			data.setId(coupon.getId());
			if (coupon.getSetting().getItems() != null) {
				data.setItems(coupon.getSetting().getItems().stream().map(item -> item.asDTO(false, false, false))
						.collect(Collectors.toList()));
			}
			if (coupon.getSetting().getCategories() != null) {
				data.setCategories(coupon.getSetting().getCategories().stream().map(category -> category.asDTO())
						.collect(Collectors.toList()));
			}
			return data;
		}).collect(Collectors.toList());
	}

}
