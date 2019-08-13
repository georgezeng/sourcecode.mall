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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.alibaba.druid.util.StringUtils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.merchant.Merchant;
import com.sourcecode.malls.domain.merchant.MerchantShopApplication;
import com.sourcecode.malls.dto.client.ClientDTO;
import com.sourcecode.malls.dto.query.PageInfo;
import com.sourcecode.malls.enums.Sex;
import com.sourcecode.malls.properties.SuperAdminProperties;
import com.sourcecode.malls.repository.jpa.impl.client.ClientRepository;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantRepository;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantShopApplicationRepository;
import com.sourcecode.malls.service.FileOnlineSystemService;
import com.sourcecode.malls.service.base.JpaService;
import com.sourcecode.malls.util.AssertUtil;
import com.sourcecode.malls.util.ImageUtil;

@Service("ClientDetailsService")
@Transactional
public class ClientService implements UserDetailsService, JpaService<Client, Long> {
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
	private RestTemplate httpClient;

	@Value("${share.image.background.path}")
	private String shareBgPath;

	@Value("${font.path}")
	private String fontPath;

	@Value("${user.avatar.default.path}")
	private String userAvatarDefaultPath;

	@Value("${user.type.name}")
	private String userDir;

	@Autowired
	private PasswordEncoder pwdEncoder;

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

	public InputStream generateQRCodeImage(String url, int width, int height) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Map<EncodeHintType, Object> hintMap = new HashMap<EncodeHintType, Object>();
		hintMap.put(EncodeHintType.MARGIN, new Integer(1));
		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, width, height, hintMap);
		MatrixToImageWriter.writeToStream(bitMatrix, "PNG", out);
		return new ByteArrayInputStream(out.toByteArray());
	}

	public byte[] loadPoster(Long userId) throws Exception {
		Optional<Client> clientOp = clientRepository.findById(userId);
		AssertUtil.assertTrue(clientOp.isPresent(), "用户不存在");
		Client client = clientOp.get();
		String nickname = client.getNickname();
		if (StringUtils.isEmpty(nickname)) {
			nickname = "****" + client.getUsername().substring(7);
		}
		String suffix = DigestUtils.md5Hex(userId + "_" + nickname + "_" + client.getAvatar()) + ".png";
		String posterPath = userDir + "/" + userId + "/invite/poster_" + suffix;
		try {
			return fileService.load(true, posterPath);
		} catch (Exception e) {
			Optional<MerchantShopApplication> app = merchantShopRepository
					.findByMerchantId(client.getMerchant().getId());
			AssertUtil.assertTrue(clientOp.isPresent(), "商铺信息不存在");
			String shareQrCodePath = userDir + "/" + userId + "/invite/qrcode_" + suffix;
			InputStream in = null;
			try {
				in = new ByteArrayInputStream(fileService.load(true, shareQrCodePath));
			} catch (Exception e1) {
				String shareQrCodeUrl = "https://" + app.get().getDomain() + "/?uid=" + userId + "#/Home";
				in = generateQRCodeImage(shareQrCodeUrl, 250, 250);
			}
			BufferedImage qrCode = ImageIO.read(in);
			String avatar = client.getAvatar();
			in = null;
			if (StringUtils.isEmpty(avatar)) {
				in = new ByteArrayInputStream(fileService.load(true, userAvatarDefaultPath));
			} else if (avatar.startsWith("http")) {
				in = new ByteArrayInputStream(httpClient.getForEntity(avatar, byte[].class).getBody());
			} else {
				in = new ByteArrayInputStream(fileService.load(true, avatar));
			}
//			String shopName = app.get().getName();
			int avatarSize = 160;
			BufferedImage avatarImage = ImageIO.read(in);
			if (avatarImage.getWidth() > avatarImage.getHeight()) {
				avatarImage = ImageUtil.rotateImage(avatarImage, 90);
				avatarImage = ImageUtil.resizeImage(avatarImage, avatarSize, avatarSize);
			}
			BufferedImage result = ImageIO.read(new ByteArrayInputStream(fileService.load(true, shareBgPath)));
			Graphics2D g = (Graphics2D) result.getGraphics();
			g.drawImage(qrCode, 240, 770, null);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Font font = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(fileService.load(true, fontPath)));
			g.setColor(Color.DARK_GRAY);
			ImageUtil.drawCenteredString(g, nickname, 0, 170, result.getWidth(), 45,
					font.deriveFont(30f).deriveFont(Font.BOLD));
//			g.setColor(Color.RED);
//			shopName = "邀请您注册" + shopName;
//			drawCenteredString(g, shopName, 0, 320, result.getWidth(), 50, font.deriveFont(50f).deriveFont(Font.BOLD));
			g.setClip(new Ellipse2D.Float(300, 10, avatarSize, avatarSize));
			g.drawImage(avatarImage, 300, 10, avatarSize, avatarSize, null);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(result, "png", out);
			byte[] arr = out.toByteArray();
			fileService.upload(true, posterPath, new ByteArrayInputStream(arr));
			return arr;
		}
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

}
