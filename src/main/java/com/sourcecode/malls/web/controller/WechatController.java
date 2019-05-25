package com.sourcecode.malls.web.controller;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sourcecode.malls.constants.SystemConstant;
import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.merchant.Merchant;
import com.sourcecode.malls.domain.merchant.MerchantShopApplication;
import com.sourcecode.malls.domain.redis.CodeStore;
import com.sourcecode.malls.dto.LoginInfo;
import com.sourcecode.malls.dto.WechatAccessInfo;
import com.sourcecode.malls.dto.WechatJsApiConfig;
import com.sourcecode.malls.dto.WechatUserInfo;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.setting.DeveloperSettingDTO;
import com.sourcecode.malls.enums.Sex;
import com.sourcecode.malls.exception.BusinessException;
import com.sourcecode.malls.repository.jpa.impl.client.ClientRepository;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantShopApplicationRepository;
import com.sourcecode.malls.repository.redis.impl.CodeStoreRepository;
import com.sourcecode.malls.service.FileOnlineSystemService;
import com.sourcecode.malls.service.impl.MerchantSettingService;
import com.sourcecode.malls.service.impl.VerifyCodeService;
import com.sourcecode.malls.util.AssertUtil;

@RestController
@RequestMapping(path = "/client/wechat")
public class WechatController {
	Logger logger = LoggerFactory.getLogger(getClass());

	private static final String WECHAT_REGISTER_TIME_ATTR = "wechat-register-code-time";
	private static final String WECHAT_REGISTER_CATEGORY = "wechat-register-category";
	private static final String WECHAT_USERINFO_ATTR = "wechat-userinfo";
	private static final String WECHAT_JSAPI_TICKET_CATEGORY = "wechat-jsapi-ticket-category";

	@Autowired
	private VerifyCodeService verifyCodeService;

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private CodeStoreRepository codeStoreRepository;

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

	@Value("${user.type.name}")
	private String userDir;

	@Autowired
	private MerchantShopApplicationRepository applicationRepository;

	@Autowired
	private MerchantSettingService settingService;

	@RequestMapping(path = "/jsconfig")
	public ResultBean<WechatJsApiConfig> getJsConfig(@RequestParam String url) throws Exception {
		Optional<DeveloperSettingDTO> setting = settingService.loadWechatGzh(ClientContext.getMerchantId());
		AssertUtil.assertTrue(setting.isPresent(), "商户信息不存在，请联系商城客服");
		String key = "merchant_" + ClientContext.getMerchantId();
		Optional<CodeStore> storeOp = codeStoreRepository.findByCategoryAndKey(WECHAT_JSAPI_TICKET_CATEGORY, key);
		CodeStore store = null;
		if (!storeOp.isPresent()) {
			String result = httpClient.getForObject(String.format(apiAccessTokenUrl, setting.get().getAccount(), setting.get().getSecret()),
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
	public ResultBean<String> fetchFile(@PathVariable String mediaId, @RequestParam(name = "filePath") String filePath) throws Exception {
		Optional<DeveloperSettingDTO> setting = settingService.loadWechatGzh(ClientContext.getMerchantId());
		AssertUtil.assertTrue(setting.isPresent(), "商户信息不存在，请联系商城客服");
		String result = httpClient.getForObject(String.format(apiAccessTokenUrl, setting.get().getAccount(), setting.get().getSecret()),
				String.class);
		WechatAccessInfo accessInfo = mapper.readValue(result, WechatAccessInfo.class);
		if (!StringUtils.isEmpty(accessInfo.getErrcode()) && !"0".equals(accessInfo.getErrcode())) {
			logger.warn("wechat error: [" + accessInfo.getErrcode() + "] - " + accessInfo.getErrmsg());
			throw new BusinessException("获取微信信息有误");
		}
		byte[] buf = httpClient.getForEntity(String.format(fileApiUrl, accessInfo.getAccessToken(), mediaId), byte[].class).getBody();
		String fileRelativePath = filePath;
		filePath = userDir + "/" + ClientContext.get().getId() + "/" + filePath;
		fileService.upload(false, filePath, new ByteArrayInputStream(buf));
		ClientContext.get().setAvatar(fileRelativePath);
		clientRepository.save(ClientContext.get());
		return new ResultBean<>();
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
		AssertUtil.assertTrue(developerSetting.isPresent(), "商户不存在");
		String token = UUID.randomUUID().toString().replaceAll("-", "");
		CodeStore store = new CodeStore();
		store.setCategory(SystemConstant.WECHAT_TOKEN_CATEGORY);
		store.setKey(token);
		store.setValue(token);
		codeStoreRepository.save(store);
		String url = String.format(loginUrl, developerSetting.get().getAccount(), URLEncoder.encode(origin + "/#/WechatLogin", "UTF-8"), token);
		return new ResultBean<>(url);
	}

	@RequestMapping(path = "/info")
	public ResultBean<LoginInfo> getWechatInfo(HttpServletRequest request, HttpSession session, @RequestBody LoginInfo loginInfo) throws Exception {
		String domain = request.getHeader("Origin").replaceAll("http(s?)://", "").replaceAll("/.*", "");
		AssertUtil.assertNotEmpty(domain, "商户不存在");
		Optional<MerchantShopApplication> apOp = applicationRepository.findByDomain(domain);
		AssertUtil.assertTrue(apOp.isPresent(), "商户不存在");
		Merchant merchant = apOp.get().getMerchant();
		Optional<DeveloperSettingDTO> developerSetting = settingService.loadWechatGzh(merchant.getId());
		AssertUtil.assertTrue(developerSetting.isPresent(), "商户不存在");
		Optional<CodeStore> store = codeStoreRepository.findByCategoryAndKey(SystemConstant.WECHAT_TOKEN_CATEGORY, loginInfo.getUsername());
		AssertUtil.assertTrue(store.isPresent(), "登录信息有误");
		String result = httpClient.getForObject(
				String.format(accessTokenUrl, developerSetting.get().getAccount(), developerSetting.get().getSecret(), loginInfo.getPassword()),
				String.class);
		WechatAccessInfo accessInfo = mapper.readValue(result, WechatAccessInfo.class);
		if (!StringUtils.isEmpty(accessInfo.getErrcode()) && !"0".equals(accessInfo.getErrcode())) {
			logger.warn("wechat error: [" + accessInfo.getErrcode() + "] - " + accessInfo.getErrmsg());
			throw new BusinessException("获取微信信息有误");
		}
		byte[] buf = httpClient.getForObject(String.format(userInfoUrl, accessInfo.getAccessToken(), accessInfo.getOpenId()), byte[].class);
		result = new String(buf, "UTF-8");
		logger.info(result);
		WechatUserInfo userInfo = mapper.readValue(result, WechatUserInfo.class);
		if (!StringUtils.isEmpty(userInfo.getErrcode()) && !"0".equals(userInfo.getErrcode())) {
			logger.warn("wechat error: [" + userInfo.getErrcode() + "] - " + userInfo.getErrmsg());
			throw new BusinessException("获取微信信息有误");
		}
		session.setAttribute(WECHAT_USERINFO_ATTR, userInfo);
		Optional<Client> user = clientRepository.findByMerchantAndUnionId(merchant, userInfo.getUnionId());
		LoginInfo info = null;
		if (user.isPresent()) {
			info = new LoginInfo();
			info.setUsername(user.get().getUsername());
			info.setPassword(loginInfo.getUsername());
		}
		return new ResultBean<>(info);
	}

	@RequestMapping(path = "/code/{mobile}")
	public ResultBean<Void> sendWechatRegisterCode(@PathVariable String mobile) {
		verifyCodeService.sendRegisterCode(mobile, WECHAT_REGISTER_TIME_ATTR, WECHAT_REGISTER_CATEGORY, ClientContext.getMerchantId() + "");
		return new ResultBean<>();
	}

	@RequestMapping(path = "/register")
	public ResultBean<Void> wechatRegister(HttpServletRequest request, HttpSession session, @RequestBody LoginInfo mobileInfo) throws Exception {
		AssertUtil.assertNotEmpty(mobileInfo.getUsername(), "手机号不能为空");
		AssertUtil.assertNotEmpty(mobileInfo.getPassword(), "验证码不能为空");
		Optional<CodeStore> codeStoreOp = codeStoreRepository.findByCategoryAndKey(WECHAT_REGISTER_CATEGORY,
				mobileInfo.getUsername() + "_" + ClientContext.getMerchantId());
		AssertUtil.assertTrue(codeStoreOp.isPresent(), "验证码无效");
		AssertUtil.assertTrue(codeStoreOp.get().getValue().equals(mobileInfo.getPassword()), "验证码无效");
		String domain = request.getHeader("Origin").replaceAll("http(s?)://", "").replaceAll("/.*", "");
		AssertUtil.assertNotEmpty(domain, "商户不存在");
		Optional<MerchantShopApplication> apOp = applicationRepository.findByDomain(domain);
		AssertUtil.assertTrue(apOp.isPresent(), "商户不存在");
		Merchant merchant = apOp.get().getMerchant();
		Optional<Client> userOp = clientRepository.findByMerchantAndUsername(merchant, mobileInfo.getUsername());
		AssertUtil.assertTrue(!userOp.isPresent(), "手机号已存在");
		WechatUserInfo userInfo = (WechatUserInfo) session.getAttribute(WECHAT_USERINFO_ATTR);
		Client user = new Client();
		user.setUsername(mobileInfo.getUsername());
		user.setUnionId(userInfo.getUnionId());
		user.setAvatar(userInfo.getHeadImgUrl());
		user.setEnabled(true);
		user.setMerchant(merchant);
		user.setNickname(userInfo.getNickname());
		user.setSex(userInfo.getSex() == 1 ? Sex.Male : Sex.Female);
		clientRepository.save(user);
		return new ResultBean<>();
	}
}
