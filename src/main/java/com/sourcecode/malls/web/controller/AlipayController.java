package com.sourcecode.malls.web.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sourcecode.malls.constants.EnvConstant;
import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.merchant.MerchantShopApplication;
import com.sourcecode.malls.domain.order.Order;
import com.sourcecode.malls.domain.redis.CodeStore;
import com.sourcecode.malls.dto.setting.DeveloperSettingDTO;
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
import com.sourcecode.malls.util.AssertUtil;

@RestController
@RequestMapping(path = "/client/alipay")
public class AlipayController {
	Logger logger = LoggerFactory.getLogger(getClass());

	private static final String ALIPAY_TOKEN_CATEGORY = "alipay-token-category";

	@Value("${alipay.api.url.gateway}")
	private String gateway;

	@Value("${alipay.api.public.key}")
	private String publicKey;

	@Value("${alipay.api.url.pay.notify}")
	private String notifyUrl;

	private String charset = "UTF-8";

	@Autowired
	private MerchantSettingService settingService;

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

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderService orderService;

	@Autowired
	private Environment env;

	@Autowired
	private MerchantShopApplicationRepository merchantShopRepository;


	@RequestMapping(path = "/prepare/params/{id}")
	public String prepare(HttpServletRequest httpRequest, @PathVariable Long id) throws ServletException, IOException {
		Optional<DeveloperSettingDTO> setting = settingService.loadAlipay(ClientContext.getMerchantId());
		AssertUtil.assertTrue(setting.isPresent(), "找不到商家信息");
		AlipayClient alipayClient = new DefaultAlipayClient(gateway, setting.get().getAccount(),
				setting.get().getSecret(), "json", charset, publicKey, "RSA2"); // 获得初始化的AlipayClient
		AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();// 创建API对应的request
		alipayRequest.setReturnUrl(httpRequest.getHeader("Origin") + "#/alipaySuccess");
		alipayRequest.setNotifyUrl(notifyUrl);// 在公共参数中设置回跳和通知地址

		Optional<Order> orderOp = orderRepository.findById(id);
		AssertUtil.assertTrue(
				orderOp.isPresent() && orderOp.get().getClient().getId().equals(ClientContext.get().getId()), "订单不存在");
		Order order = orderOp.get();
		String token = UUID.randomUUID().toString().replaceAll("-", "");
		CodeStore tokenStore = new CodeStore();
		tokenStore.setCategory(ALIPAY_TOKEN_CATEGORY);
		tokenStore.setKey(token);
		tokenStore.setValue(order.getOrderId());
		codeStoreRepository.save(tokenStore);

		Map<String, String> data = new HashMap<>();
		data.put("out_trade_no", token);
		if (env.acceptsProfiles(Profiles.of(EnvConstant.PROD))) {
			data.put("total_amount", order.getTotalPrice() + "");
		} else {
			data.put("total_amount", new BigDecimal(order.getSubList().size()).multiply(new BigDecimal("0.01")) + "");
		}
		Optional<MerchantShopApplication> shop = merchantShopRepository.findByMerchantId(ClientContext.getMerchantId());
		data.put("subject", "[" + shop.get().getName() + "]商品订单支付");
		alipayRequest.setBizContent(mapper.writeValueAsString(data));// 填充业务参数
		String form = "";
		try {
			form = alipayClient.pageExecute(alipayRequest).getBody(); // 调用SDK生成表单
		} catch (AlipayApiException e) {
			throw new BusinessException(e.getMessage(), e);
		}
		return form;
	}
	
	@RequestMapping(path = "/notify/paySuccess")
	public void prepare(@RequestBody String payload) throws ServletException, IOException {
		logger.info(payload);
		
	}
}
