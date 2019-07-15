package com.sourcecode.malls.web.controller;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayUtil;
import com.github.wxpay.sdk.WePayConfig;
import com.sourcecode.malls.constants.EnvConstant;
import com.sourcecode.malls.constants.ExceptionMessageConstant;
import com.sourcecode.malls.constants.SystemConstant;
import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.client.WechatToken;
import com.sourcecode.malls.domain.merchant.Merchant;
import com.sourcecode.malls.domain.merchant.MerchantShopApplication;
import com.sourcecode.malls.domain.order.Order;
import com.sourcecode.malls.domain.redis.CodeStore;
import com.sourcecode.malls.dto.LoginInfo;
import com.sourcecode.malls.dto.WechatJsApiConfig;
import com.sourcecode.malls.dto.WechatUserInfo;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.setting.DeveloperSettingDTO;
import com.sourcecode.malls.dto.wechat.WechatAccessInfo;
import com.sourcecode.malls.enums.Sex;
import com.sourcecode.malls.exception.BusinessException;
import com.sourcecode.malls.repository.jpa.impl.client.ClientRepository;
import com.sourcecode.malls.repository.jpa.impl.client.WechatTokenRepository;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantShopApplicationRepository;
import com.sourcecode.malls.repository.jpa.impl.order.OrderRepository;
import com.sourcecode.malls.repository.redis.impl.CodeStoreRepository;
import com.sourcecode.malls.service.FileOnlineSystemService;
import com.sourcecode.malls.service.impl.MerchantSettingService;
import com.sourcecode.malls.service.impl.OrderService;
import com.sourcecode.malls.service.impl.VerifyCodeService;
import com.sourcecode.malls.service.impl.WechatSettingService;
import com.sourcecode.malls.util.AssertUtil;

@RestController
@RequestMapping(path = "/client/wechat")
public class WechatController {
	Logger logger = LoggerFactory.getLogger(getClass());

	private static final String WECHAT_REGISTER_CATEGORY = "wechat-register-category";
	private static final String WECHAT_JSAPI_TICKET_CATEGORY = "wechat-jsapi-ticket-category";
	private static final String WECHAT_USERINFO_CATEGORY = "wechat-userinfo-category";
	private static final String WECHAT_PAY_TOKEN_CATEGORY = "wechat-pay-token-category";

	@Autowired
	private VerifyCodeService verifyCodeService;

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private CodeStoreRepository codeStoreRepository;

	@Autowired
	private WechatTokenRepository wechatTokenRepository;

	@Autowired
	private RestTemplate httpClient;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private FileOnlineSystemService fileService;

	@Value("${wechat.user.url.login}")
	private String loginUrl;

	@Value("${wechat.user.url.access_token}")
	private String accessTokenUrl;

	@Value("${wechat.user.url.userinfo}")
	private String userInfoUrl;

	@Value("${wechat.api.url.access_token}")
	private String apiAccessTokenUrl;

	@Value("${wechat.api.url.js}")
	private String jsApiUrl;

	@Value("${wechat.api.url.file}")
	private String fileApiUrl;

	@Value("${wechat.api.url.pay.notify_url}")
	private String notifyUrl;

	@Value("${user.type.name}")
	private String userDir;

	@Autowired
	private MerchantShopApplicationRepository applicationRepository;

	@Autowired
	private MerchantSettingService settingService;

	@Autowired
	private WechatSettingService wechatSettingService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderService orderService;

	@Autowired
	private Environment env;

	@Autowired
	private MerchantShopApplicationRepository merchantShopRepository;

	@RequestMapping(path = "/jsconfig")
	public ResultBean<WechatJsApiConfig> getJsConfig(@RequestParam String url) throws Exception {
		Optional<DeveloperSettingDTO> setting = settingService.loadWechatGzh(ClientContext.getMerchantId());
		AssertUtil.assertTrue(setting.isPresent(), "商户信息不存在，请联系商城客服");
		String key = "merchant_" + ClientContext.getMerchantId();
		Optional<CodeStore> storeOp = codeStoreRepository.findByCategoryAndKey(WECHAT_JSAPI_TICKET_CATEGORY, key);
		CodeStore store = null;
		if (!storeOp.isPresent()) {
			String result = httpClient.getForObject(
					String.format(apiAccessTokenUrl, setting.get().getAccount(), setting.get().getSecret()),
					String.class);
			WechatAccessInfo accessInfo = mapper.readValue(result, WechatAccessInfo.class);
			if (!StringUtils.isEmpty(accessInfo.getErrcode()) && !"0".equals(accessInfo.getErrcode())) {
				logger.warn("wechat error: [" + accessInfo.getErrcode() + "] - " + accessInfo.getErrmsg());
				throw new BusinessException("获取微信信息有误");
			}
			result = httpClient.getForObject(String.format(jsApiUrl, accessInfo.getAccessToken()), String.class);
			accessInfo = mapper.readValue(result, WechatAccessInfo.class);
			if (!StringUtils.isEmpty(accessInfo.getErrcode()) && !"0".equals(accessInfo.getErrcode())) {
				logger.warn("wechat error: [" + accessInfo.getErrcode() + "] - " + accessInfo.getErrmsg());
				throw new BusinessException("获取微信信息有误");
			}
			store = new CodeStore();
			store.setCategory(WECHAT_JSAPI_TICKET_CATEGORY);
			store.setKey(key);
			store.setValue(accessInfo.getTicket());
			codeStoreRepository.save(store);
		} else {
			store = storeOp.get();
		}
		String nonce = UUID.randomUUID().toString();
		Long timestamp = new Date().getTime();
		String template = "jsapi_ticket=%s&noncestr=%s&timestamp=%s&url=%s";
		String signature = String.format(template, store.getValue(), nonce, timestamp + "", url);
		signature = DigestUtils.sha1Hex(signature);
		WechatJsApiConfig ticket = new WechatJsApiConfig();
		ticket.setAppId(setting.get().getAccount());
		ticket.setNonce(nonce);
		ticket.setTimestamp(timestamp);
		ticket.setSignature(signature);
		return new ResultBean<>(ticket);
	}

	@RequestMapping(path = "/fetchFile/{mediaId}")
	public ResultBean<String> fetchFile(@PathVariable String mediaId, @RequestParam(name = "filePath") String filePath)
			throws Exception {
		Optional<DeveloperSettingDTO> setting = settingService.loadWechatGzh(ClientContext.getMerchantId());
		AssertUtil.assertTrue(setting.isPresent(), "商户信息不存在，请联系商城客服");
		String result = httpClient.getForObject(
				String.format(apiAccessTokenUrl, setting.get().getAccount(), setting.get().getSecret()), String.class);
		WechatAccessInfo accessInfo = mapper.readValue(result, WechatAccessInfo.class);
		if (!StringUtils.isEmpty(accessInfo.getErrcode()) && !"0".equals(accessInfo.getErrcode())) {
			logger.warn("wechat error: [" + accessInfo.getErrcode() + "] - " + accessInfo.getErrmsg());
			throw new BusinessException("获取微信信息有误");
		}
		byte[] buf = httpClient
				.getForEntity(String.format(fileApiUrl, accessInfo.getAccessToken(), mediaId), byte[].class).getBody();
		filePath = userDir + "/" + ClientContext.get().getId() + "/" + filePath;
		fileService.upload(false, filePath, new ByteArrayInputStream(buf));
		return new ResultBean<>(filePath);
	}

	@RequestMapping(path = "/loginUrl")
	public ResultBean<String> wechatLoginUrl(HttpServletRequest request) throws Exception {
		String origin = request.getHeader("Origin");
		String domain = origin.replaceAll("http(s?)://", "").replaceAll("/.*", "");
		AssertUtil.assertNotEmpty(domain, "商户不存在");
		Optional<MerchantShopApplication> apOp = applicationRepository.findByDomain(domain);
		AssertUtil.assertTrue(apOp.isPresent(), "商户不存在");
		Merchant merchant = apOp.get().getMerchant();
		Optional<DeveloperSettingDTO> developerSetting = settingService.loadWechatGzh(merchant.getId());
		AssertUtil.assertTrue(developerSetting.isPresent(), "商户信息不存在");
		String token = UUID.randomUUID().toString().replaceAll("-", "");
		CodeStore store = new CodeStore();
		store.setCategory(SystemConstant.WECHAT_TOKEN_CATEGORY);
		store.setKey(token);
		store.setValue(token);
		codeStoreRepository.save(store);
		String url = String.format(loginUrl, developerSetting.get().getAccount(),
				URLEncoder.encode(origin + "/#/WechatLogin", "UTF-8"), token);
		return new ResultBean<>(url);
	}

	@RequestMapping(path = "/loginInfo")
	public ResultBean<LoginInfo> getLoginInfo(HttpServletRequest request, @RequestBody LoginInfo loginInfo)
			throws Exception {
		String domain = request.getHeader("Origin").replaceAll("http(s?)://", "").replaceAll("/.*", "");
		AssertUtil.assertNotEmpty(domain, "商户不存在");
		Optional<MerchantShopApplication> apOp = applicationRepository.findByDomain(domain);
		AssertUtil.assertTrue(apOp.isPresent(), "商户不存在");
		Merchant merchant = apOp.get().getMerchant();
		Optional<DeveloperSettingDTO> developerSetting = settingService.loadWechatGzh(merchant.getId());
		AssertUtil.assertTrue(developerSetting.isPresent(), "商户不存在");
		Optional<CodeStore> storeOp = codeStoreRepository.findByCategoryAndKey(SystemConstant.WECHAT_TOKEN_CATEGORY,
				loginInfo.getUsername());
		AssertUtil.assertTrue(storeOp.isPresent(), "登录信息有误");
		String result = httpClient.getForObject(String.format(accessTokenUrl, developerSetting.get().getAccount(),
				developerSetting.get().getSecret(), loginInfo.getPassword()), String.class);
		WechatAccessInfo accessInfo = mapper.readValue(result, WechatAccessInfo.class);
		if (!StringUtils.isEmpty(accessInfo.getErrcode()) && !"0".equals(accessInfo.getErrcode())) {
			logger.warn("wechat error: [" + accessInfo.getErrcode() + "] - " + accessInfo.getErrmsg());
			throw new BusinessException("获取微信信息有误");
		}
		byte[] buf = httpClient.getForObject(
				String.format(userInfoUrl, accessInfo.getAccessToken(), accessInfo.getOpenId()), byte[].class);
		result = new String(buf, "UTF-8");
		WechatUserInfo userInfo = mapper.readValue(result, WechatUserInfo.class);
		userInfo.setAccessToken(accessInfo.getAccessToken());
		userInfo.setRefreshToken(accessInfo.getRefreshToken());
		if (!StringUtils.isEmpty(userInfo.getErrcode()) && !"0".equals(userInfo.getErrcode())) {
			logger.warn("wechat error: [" + userInfo.getErrcode() + "] - " + userInfo.getErrmsg());
			throw new BusinessException("获取微信信息有误");
		}
		CodeStore store = new CodeStore();
		store.setCategory(WECHAT_USERINFO_CATEGORY);
		store.setKey(loginInfo.getUsername());
		store.setValue(mapper.writeValueAsString(userInfo));
		codeStoreRepository.save(store);
		Optional<Client> user = clientRepository.findByMerchantAndUnionId(merchant, userInfo.getUnionId());
		LoginInfo info = null;
		if (user.isPresent()) {
			Optional<WechatToken> tokensOp = wechatTokenRepository.findByUserId(user.get().getId());
			WechatToken tokens = null;
			if (!tokensOp.isPresent()) {
				tokens = new WechatToken();
				tokens.setUserId(user.get().getId());
			} else {
				tokens = tokensOp.get();
			}
			tokens.setAccessToken(accessInfo.getAccessToken());
			tokens.setOpenId(accessInfo.getOpenId());
			tokens.setRefreshToken(accessInfo.getRefreshToken());
			wechatTokenRepository.save(tokens);
			info = new LoginInfo();
			info.setUsername(user.get().getUsername());
			info.setPassword(loginInfo.getUsername());
		}
		return new ResultBean<>(info);
	}

	@RequestMapping(path = "/code/{mobile}")
	public ResultBean<Void> sendWechatRegisterCode(@PathVariable String mobile) {
		verifyCodeService.sendRegisterCode(mobile, WECHAT_REGISTER_CATEGORY, ClientContext.getMerchantId() + "");
		return new ResultBean<>();
	}

	@RequestMapping(path = "/register")
	public ResultBean<Void> wechatRegister(HttpServletRequest request, @RequestBody LoginInfo mobileInfo)
			throws Exception {
		AssertUtil.assertNotEmpty(mobileInfo.getUsername(), "手机号不能为空");
		AssertUtil.assertNotEmpty(mobileInfo.getPassword(), "验证码不能为空");
		Optional<CodeStore> codeStoreOp = codeStoreRepository.findByCategoryAndKey(WECHAT_REGISTER_CATEGORY,
				mobileInfo.getUsername() + "_" + ClientContext.getMerchantId());
		AssertUtil.assertTrue(codeStoreOp.isPresent(), ExceptionMessageConstant.VERIFY_CODE_INVALID);
		AssertUtil.assertTrue(codeStoreOp.get().getValue().equals(mobileInfo.getPassword()),
				ExceptionMessageConstant.VERIFY_CODE_INVALID);
		String domain = request.getHeader("Origin").replaceAll("http(s?)://", "").replaceAll("/.*", "");
		AssertUtil.assertNotEmpty(domain, "商户不存在");
		Optional<MerchantShopApplication> apOp = applicationRepository.findByDomain(domain);
		AssertUtil.assertTrue(apOp.isPresent(), "商户不存在");
		Merchant merchant = apOp.get().getMerchant();
		Optional<Client> userOp = clientRepository.findByMerchantAndUsername(merchant, mobileInfo.getUsername());
		AssertUtil.assertTrue(!userOp.isPresent(), "手机号已存在");
		codeStoreOp = codeStoreRepository.findByCategoryAndKey(WECHAT_USERINFO_CATEGORY, mobileInfo.getToken());
		AssertUtil.assertTrue(codeStoreOp.isPresent(), "无法获取微信信息");
		WechatUserInfo userInfo = mapper.readValue(codeStoreOp.get().getValue(), WechatUserInfo.class);
		Client user = new Client();
		user.setUsername(mobileInfo.getUsername());
		user.setUnionId(userInfo.getUnionId());
		user.setAvatar(userInfo.getHeadImgUrl());
		user.setEnabled(true);
		user.setMerchant(merchant);
		user.setNickname(userInfo.getNickname());
		switch (userInfo.getSex()) {
		case 1:
			user.setSex(Sex.Male);
			break;
		case 2:
			user.setSex(Sex.Female);
			break;
		default:
			user.setSex(Sex.Secret);
		}
		clientRepository.save(user);
		WechatToken tokens = new WechatToken();
		tokens.setUserId(user.getId());
		tokens.setAccessToken(userInfo.getAccessToken());
		tokens.setRefreshToken(userInfo.getRefreshToken());
		tokens.setOpenId(userInfo.getOpenId());
		wechatTokenRepository.save(tokens);
		return new ResultBean<>();
	}

	@RequestMapping(path = "/pay/success")
	public void notify(@RequestBody String payload) throws Exception {
		String orderId = WXPayUtil.xmlToMap(payload).get("out_trade_no");
		orderService.afterPayment(orderId);
	}

	@RequestMapping(path = "/unifiedOrder")
	public ResultBean<Map<String, String>> unifiedOrder(HttpServletRequest request,
			@RequestBody Map<String, String> params) throws Exception {
		String ip = request.getHeader("X-Forwarded-For");
		if (StringUtils.isEmpty(ip)) {
			ip = request.getRemoteAddr();
		}
		Optional<Order> orderOp = orderRepository.findById(Long.valueOf(params.get("id")));
		AssertUtil.assertTrue(
				orderOp.isPresent() && orderOp.get().getClient().getId().equals(ClientContext.get().getId()), "订单不存在");
		Order order = orderOp.get();
		Optional<MerchantShopApplication> shop = merchantShopRepository.findByMerchantId(ClientContext.getMerchantId());
		WePayConfig config = wechatSettingService.createWePayConfig(ClientContext.getMerchantId());
		WXPay wxpay = new WXPay(config);
		Map<String, String> data = new HashMap<String, String>();
		data.put("body", "[" + shop.get().getName() + "]商品订单支付");
		data.put("out_trade_no", order.getOrderId());
		data.put("device_info", "WEB");
		data.put("fee_type", "CNY");
		if (env.acceptsProfiles(Profiles.of(EnvConstant.PROD))) {
			data.put("total_fee", order.getTotalPrice().multiply(new BigDecimal("100")).intValue() + "");
		} else {
			data.put("total_fee", "1");
		}
		data.put("spbill_create_ip", ip);
		String token = UUID.randomUUID().toString();
		CodeStore tokenStore = new CodeStore();
		tokenStore.setCategory(WECHAT_PAY_TOKEN_CATEGORY);
		tokenStore.setKey(token);
		tokenStore.setValue(order.getOrderId());
		codeStoreRepository.save(tokenStore);
		data.put("notify_url", notifyUrl);
		data.put("trade_type", params.get("type"));
		if ("JSAPI".equals(params.get("type"))) {
			Optional<WechatToken> wechatToken = wechatTokenRepository.findByUserId(ClientContext.get().getId());
			AssertUtil.assertTrue(wechatToken.isPresent(), "无法获取微信账号信息，请重新登录");
			data.put("openid", wechatToken.get().getOpenId());
		}

		Map<String, String> resp = wxpay.unifiedOrder(data);
		AssertUtil.assertTrue("SUCCESS".equals(resp.get("return_code")), "支付失败: " + resp.get("return_msg"));
		AssertUtil.assertTrue("SUCCESS".equals(resp.get("result_code")), "支付失败: " + resp.get("err_code_des"));

		String timestamp = new Date().getTime() / 1000 + "";
		String template = "appId=%s&nonceStr=%s&package=prepay_id=%s&signType=MD5&timeStamp=%s&key=%s";
		String signature = String.format(template, config.getAppID(), resp.get("nonce_str"), resp.get("prepay_id"),
				timestamp, config.getKey());
		signature = DigestUtils.md5Hex(signature).toUpperCase();
		resp.put("timestamp", timestamp);
		resp.put("package", "prepay_id=" + resp.get("prepay_id"));
		resp.put("paySign", signature);
		return new ResultBean<>(resp);
	}
}
