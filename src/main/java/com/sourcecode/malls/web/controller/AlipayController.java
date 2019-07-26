package com.sourcecode.malls.web.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.sourcecode.malls.config.AlipayConfig;
import com.sourcecode.malls.constants.EnvConstant;
import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.merchant.MerchantShopApplication;
import com.sourcecode.malls.domain.order.Order;
import com.sourcecode.malls.domain.redis.CodeStore;
import com.sourcecode.malls.dto.setting.DeveloperSettingDTO;
import com.sourcecode.malls.enums.OrderStatus;
import com.sourcecode.malls.exception.BusinessException;
import com.sourcecode.malls.repository.jpa.impl.client.ClientRepository;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantShopApplicationRepository;
import com.sourcecode.malls.repository.jpa.impl.order.OrderRepository;
import com.sourcecode.malls.repository.redis.impl.CodeStoreRepository;
import com.sourcecode.malls.service.impl.MerchantSettingService;
import com.sourcecode.malls.service.impl.OrderService;
import com.sourcecode.malls.util.AssertUtil;

@RestController
@RequestMapping(path = "/client/alipay")
public class AlipayController {
	Logger logger = LoggerFactory.getLogger(getClass());

	private static final String ALIPAY_TOKEN_CATEGORY = "alipay-token-category";

	@Autowired
	private AlipayConfig config;

	@Value("${alipay.api.url.pay.notify}")
	private String notifyUrl;

	@Autowired
	private MerchantSettingService settingService;

	@Autowired
	private CodeStoreRepository codeStoreRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderService orderService;

	@Autowired
	private Environment env;

	@Autowired
	private MerchantShopApplicationRepository merchantShopRepository;

	@Autowired
	private ClientRepository clientRepository;

	@RequestMapping(path = "/prepare/params/{uid}/{oid}", produces = "text/html")
	public String prepare(HttpServletRequest httpRequest, @PathVariable("uid") Long userId,
			@PathVariable("oid") Long orderId, @RequestParam("to") String to) throws ServletException, IOException {
		Optional<DeveloperSettingDTO> setting = settingService.loadAlipay(ClientContext.getMerchantId());
		AssertUtil.assertTrue(setting.isPresent(), "找不到商家信息");
		Optional<MerchantShopApplication> shop = merchantShopRepository.findByMerchantId(ClientContext.getMerchantId());
		AssertUtil.assertTrue(shop.isPresent(), "找不到商家信息");
		AlipayClient alipayClient = new DefaultAlipayClient(config.getGateway(), setting.get().getAccount(),
				setting.get().getSecret(), config.getDataType(), config.getCharset(), setting.get().getMch(),
				config.getEncryptType()); // 获得初始化的AlipayClient
		AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();// 创建API对应的request
		String returnUrl = "https://" + shop.get().getDomain() + "/?uid=" + userId + "#" + to;
		alipayRequest.setReturnUrl(returnUrl);
		logger.info(returnUrl);
		alipayRequest.setNotifyUrl(notifyUrl);// 在公共参数中设置回跳和通知地址

		Optional<Order> orderOp = orderRepository.findById(orderId);
		Optional<Client> userOp = clientRepository.findById(userId);
		AssertUtil.assertTrue(orderOp.isPresent() && userOp.isPresent()
				&& orderOp.get().getClient().getId().equals(userOp.get().getId()), "订单不存在");
		Order order = orderOp.get();
		AssertUtil.assertTrue(OrderStatus.UnPay.equals(order.getStatus()), "订单状态有误，不能支付");
		String token = DigestUtils.md5Hex(order.getOrderId());
		CodeStore tokenStore = new CodeStore();
		tokenStore.setCategory(ALIPAY_TOKEN_CATEGORY);
		tokenStore.setKey(token);
		tokenStore.setValue(order.getOrderId());
		codeStoreRepository.save(tokenStore);

		AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
		model.setOutTradeNo(token);
		model.setSubject("[" + shop.get().getName() + "]商品订单支付");
		if (env.acceptsProfiles(Profiles.of(EnvConstant.PROD))) {
			model.setTotalAmount(order.getTotalPrice() + "");
		} else {
			model.setTotalAmount(new BigDecimal(order.getSubList().size()).multiply(new BigDecimal("0.01")) + "");
		}
		model.setTimeoutExpress("5m");
		model.setProductCode("QUICK_WAP_WAY");
		alipayRequest.setBizModel(model);// 填充业务参数
		String form = "";
		try {
			form = alipayClient.pageExecute(alipayRequest).getBody(); // 调用SDK生成表单
		} catch (AlipayApiException e) {
			throw new BusinessException(e.getMessage(), e);
		}
		return form;
	}

	@RequestMapping(path = "/notify/pay")
	public void prepare(@RequestParam("out_trade_no") String token, @RequestParam("trade_no") String transactionId)
			throws ServletException, IOException {
		Optional<CodeStore> tokenStore = codeStoreRepository.findByCategoryAndKey(ALIPAY_TOKEN_CATEGORY, token);
		if (tokenStore.isPresent()) {
			orderService.afterPayment(tokenStore.get().getValue(), transactionId);
			codeStoreRepository.delete(tokenStore.get());
		}
	}
}
