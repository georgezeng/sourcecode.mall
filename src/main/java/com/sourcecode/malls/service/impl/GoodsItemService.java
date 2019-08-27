package com.sourcecode.malls.service.impl;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.query.NativeQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.sourcecode.malls.constants.ExceptionMessageConstant;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.coupon.ClientCoupon;
import com.sourcecode.malls.domain.goods.GoodsItem;
import com.sourcecode.malls.domain.merchant.MerchantShopApplication;
import com.sourcecode.malls.dto.goods.GoodsItemDTO;
import com.sourcecode.malls.dto.query.PageInfo;
import com.sourcecode.malls.dto.query.QueryInfo;
import com.sourcecode.malls.repository.jpa.impl.client.ClientRepository;
import com.sourcecode.malls.repository.jpa.impl.coupon.ClientCouponRepository;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantShopApplicationRepository;
import com.sourcecode.malls.service.FileOnlineSystemService;
import com.sourcecode.malls.service.base.JpaService;
import com.sourcecode.malls.util.AssertUtil;
import com.sourcecode.malls.util.ImageUtil;

@Service
@Transactional
public class GoodsItemService extends BaseGoodsItemService implements JpaService<GoodsItem, Long> {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Value("${goods.image.share.background.big.path}")
	private String shareBigPath;

	@Value("${goods.image.share.background.small.path}")
	private String shareSmallPath;

	@Value("${goods.image.share.logo.path}")
	private String shareLogoPath;

	@Autowired
	private ImageService imageService;

	@Autowired
	private FileOnlineSystemService fileService;

	@Autowired
	private MerchantShopApplicationRepository shopRepository;

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private ClientCouponRepository clientCouponRepository;

	@Autowired
	private EntityManager em;

	@Transactional(readOnly = true)
	public List<GoodsItemDTO> findByCategory(Long merchantId, Long categoryId, String type,
			QueryInfo<String> queryInfo) {
		PageInfo pageInfo = queryInfo.getPage();
		Page<GoodsItem> pageResult = null;
		Specification<GoodsItem> spec = new Specification<GoodsItem>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Predicate toPredicate(Root<GoodsItem> root, CriteriaQuery<?> query,
					CriteriaBuilder criteriaBuilder) {
				List<Predicate> predicate = new ArrayList<>();
				predicate.add(criteriaBuilder.equal(root.get("merchant"), merchantId));
				if (categoryId > 0) {
					predicate.add(criteriaBuilder.equal(root.get("category"), categoryId));
				}
				if (!StringUtils.isEmpty(queryInfo.getData())) {
					String like = "%" + queryInfo.getData() + "%";
					predicate.add(criteriaBuilder.or(criteriaBuilder.like(root.get("name"), like),
							criteriaBuilder.like(root.get("sellingPoints"), like),
							criteriaBuilder.like(root.join("category").get("name"), like),
							criteriaBuilder.like(root.join("brand").get("name"), like)));
				}
				predicate.add(criteriaBuilder.equal(root.get("enabled"), true));
				return query.where(predicate.toArray(new Predicate[] {})).getRestriction();
			}
		};
		switch (type) {
		case "putTime":
			pageResult = itemRepository.findAll(spec, pageInfo.pageable(pageInfo.getOrder(), "putTime"));
			break;
		case "price": {
			if (Direction.ASC.name().equals(pageInfo.getOrder())) {
				pageResult = itemRepository.findAll(spec,
						pageInfo.pageable(pageInfo.getOrder(), "minPrice", "putTime"));
			} else {
				pageResult = itemRepository.findAll(spec,
						pageInfo.pageable(pageInfo.getOrder(), "maxPrice", "putTime"));
			}
		}
			break;
		default:
			pageResult = itemRepository.findAll(spec,
					pageInfo.pageable(Direction.DESC, "rank.orderNums", "rank.goodEvaluations", "putTime"));
		}
		return pageResult.get().map(it -> it.asDTO(false, false, false)).collect(Collectors.toList());
	}

	@SuppressWarnings({ "unchecked", "incomplete-switch" })
	@Transactional(readOnly = true)
	public List<GoodsItemDTO> findByCoupon(Long merchantId, Long couponId, String type, QueryInfo<String> queryInfo) {
		Optional<ClientCoupon> couponOp = clientCouponRepository.findById(couponId);
		if (!couponOp.isPresent()) {
			return new ArrayList<>();
		}
		ClientCoupon coupon = couponOp.get();
		if (!coupon.getMerchant().getId().equals(merchantId)) {
			return new ArrayList<>();
		}
//		if (!ClientCouponStatus.UnUse.equals(coupon.getStatus())
//				|| !CouponSettingStatus.PutAway.equals(coupon.getSetting().getStatus())) {
//			return Page.empty();
//		}
		List<Object> args = new ArrayList<>();
		StringBuilder fromCondition = new StringBuilder();
		fromCondition.append("from goods_item item").append("\n");
		switch (coupon.getSetting().getHxType()) {
		case Category: {
			fromCondition.append(
					"inner join coupon_setting_real_category cc on item.category_id = cc.category_id and cc.setting_id=?")
					.append("\n");
			args.add(coupon.getSetting().getId());
		}
			break;
		case Item: {
			fromCondition.append("inner join coupon_setting_goods_item ci on item.id = ci.item_id and ci.setting_id=?")
					.append("\n");
			args.add(coupon.getSetting().getId());
		}
			break;
		}
		fromCondition.append("inner join goods_brand b on item.brand_id = b.id").append("\n");
		fromCondition.append("inner join goods_item_rank r on item.id = r.item_id").append("\n");
		fromCondition.append("where item.enabled=true").append("\n");
		fromCondition.append("and item.merchant_id=?").append("\n");
		args.add(merchantId);
		if (!StringUtils.isEmpty(queryInfo.getData())) {
			String like = "%" + queryInfo.getData() + "%";
			fromCondition.append("and (item.name like ? or item.selling_points like ? or b.name like ?)").append("\n");
			for (int i = 0; i < 3; i++) {
				args.add(like);
			}
		}

//		StringBuilder countSql = new StringBuilder("select count(item.id) ");
//		countSql.append(fromCondition);
//		Query query = em.createNativeQuery(countSql.toString());
//		int pos = 1;
//		for (Object arg : args) {
//			query.setParameter(pos++, arg);
//		}
//		Long count = (Long) query.getSingleResult();
//		if (count == 0) {
//			return new PageResult<>();
//		}

		StringBuilder sql = new StringBuilder("select item.* ");
		sql.append(fromCondition);

		PageInfo pageInfo = queryInfo.getPage();
		switch (type) {
		case "putTime": {
			sql.append("order by item.put_time " + pageInfo.getOrder().toLowerCase()).append("\n");
		}
			break;
		case "price": {
			if (Direction.ASC.name().equals(pageInfo.getOrder())) {
				sql.append("order by item.min_price " + pageInfo.getOrder().toLowerCase()).append("\n");
			} else {
				sql.append("order by item.max_price " + pageInfo.getOrder().toLowerCase()).append("\n");
			}
		}
		default: {
			sql.append("order by r.order_nums, r.good_evaluations, item.put_time desc").append("\n");
		}
		}
		sql.append("limit ?, ?").append("\n");
		args.add((pageInfo.getNum() - 1) * pageInfo.getSize());
		args.add(pageInfo.getSize());
		NativeQuery<GoodsItem> query = (NativeQuery<GoodsItem>) em.createNativeQuery(sql.toString());
		int pos = 1;
		for (Object arg : args) {
			query.setParameter(pos++, arg);
		}
		query.addEntity(GoodsItem.class);
		return query.getResultStream().map(it -> it.asDTO(false, false, false))
				.collect(Collectors.toList());
	}

	@Override
	public JpaRepository<GoodsItem, Long> getRepository() {
		return itemRepository;
	}

	@CacheEvict(cacheNames = "client_invite_poster", allEntries = true)
	@Scheduled(fixedRate = 6 * 60 * 60 * 1000)
	public void clearInvitePoster() {
	}

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = "goods_item_share_poster", key = "#itemId.toString().concat('-').concat(#index.toString()).concat('-').concat(#userId.toString())")
	public byte[] loadSharePoster(Long itemId, Integer index, Long userId) throws Exception {
		Optional<Client> client = clientRepository.findById(userId);
		Optional<GoodsItem> itemOp = itemRepository.findById(itemId);
		AssertUtil.assertTrue(itemOp.isPresent(), ExceptionMessageConstant.NO_SUCH_RECORD);
		GoodsItem item = itemOp.get();
		Optional<MerchantShopApplication> app = shopRepository.findByMerchantId(item.getMerchant().getId());
		AssertUtil.assertTrue(app.isPresent(), "商铺信息不存在");
		String url = "https://" + app.get().getDomain() + "/";
		if (client.isPresent()) {
			url += "?uid=" + userId;
		}
		url += "#/Goods/Item/Detail/" + itemId;
		BufferedImage result = ImageIO.read(new ByteArrayInputStream(fileService.load(true, shareBigPath)));
		BufferedImage logo = ImageIO.read(new ByteArrayInputStream(fileService.load(true, shareLogoPath)));
		BufferedImage qrCode = ImageUtil.resizeImage(imageService.generateQRCodeImage(url, 1000, 1000, 0), 250, 250);
		BufferedImage grayBg = ImageIO.read(new ByteArrayInputStream(fileService.load(true, shareSmallPath)));
		Graphics2D g1 = (Graphics2D) grayBg.getGraphics();
		g1.drawImage(qrCode, 10, 10, null);
		g1.setColor(Color.decode("#333333"));
		g1.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		ImageUtil.drawCenteredString(g1, "长按识别二维码下单", 10, 275, qrCode.getWidth(), 30,
				imageService.getFont().deriveFont(25f).deriveFont(Font.BOLD));
		String photo = item.getThumbnail();
		if (!CollectionUtils.isEmpty(item.getPhotos())) {
			photo = item.getPhotos().get(index).getPath();
		}
		BufferedImage goodsItem = ImageUtil.resizeImage(
				ImageIO.read(new ByteArrayInputStream(fileService.load(true, photo))), result.getWidth(),
				result.getWidth());
		Graphics2D g2 = (Graphics2D) result.getGraphics();
		g2.drawImage(goodsItem, 0, 0, null);
		g2.drawImage(logo, 0, 30, null);
		g2.drawImage(grayBg, result.getWidth() * 3 / 4 - grayBg.getWidth() / 2 + 30, result.getWidth() + 50, null);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(Color.decode("#333333"));
		Font font40F = imageService.getFont().deriveFont(40f).deriveFont(Font.BOLD);
		g2.setFont(imageService.getFont().deriveFont(40f).deriveFont(Font.BOLD));
		String itemName = item.getName();
		int size = itemName.length() / 10;
		int startY = result.getWidth() + 90;
		boolean outOfBound = false;
		int line = 0;
		int lineHeight = 60;
		int lineNums = 10;
		for (; line < size; line++) {
			if (line < 2) {
				g2.drawString(itemName.substring(lineNums * line, lineNums * (line + 1)), 20,
						startY + lineHeight * line);
			} else {
				outOfBound = true;
				break;
			}
		}
		if (outOfBound) {
			int end = line * lineNums + 5;
			if (itemName.length() < end) {
				end = itemName.length();
			}
			g2.drawString(itemName.substring(line * lineNums, end) + "...", 20, startY + lineHeight * line);
		} else {
			g2.drawString(itemName.substring(line * lineNums), 20, startY + lineHeight * line);
		}
		g2.setColor(Color.decode("#D06E6D"));
		int height = result.getHeight() - (result.getHeight() - (result.getWidth() + 50 + grayBg.getHeight()));
		String realPrice = "￥" + item.getMinPrice();
		g2.drawString(realPrice, 20, height);
		if (item.getMarketPrice() != null) {
			g2.setColor(Color.decode("#919BAD"));
			FontMetrics metrics = g2.getFontMetrics(font40F);
			AttributedString as = new AttributedString("￥" + item.getMarketPrice());
			as.addAttribute(TextAttribute.FONT,
					imageService.getFont().deriveFont(30f).deriveFont(Font.BOLD).deriveFont(Font.ITALIC));
			as.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
			g2.drawString(as.getIterator(), metrics.stringWidth(realPrice) + 30, height);
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(ImageUtil.setClip(result, 40), "png", out);
		return out.toByteArray();
	}
}
