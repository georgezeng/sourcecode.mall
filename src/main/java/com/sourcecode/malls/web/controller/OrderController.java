package com.sourcecode.malls.web.controller;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.redis.CodeStore;
import com.sourcecode.malls.dto.ClientCouponDTO;
import com.sourcecode.malls.dto.OrderPreviewDTO;
import com.sourcecode.malls.dto.SettleAccountDTO;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.order.OrderDTO;
import com.sourcecode.malls.dto.query.QueryInfo;
import com.sourcecode.malls.enums.OrderStatus;
import com.sourcecode.malls.repository.redis.impl.CodeStoreRepository;
import com.sourcecode.malls.service.impl.OrderService;
import com.sourcecode.malls.util.AssertUtil;

@RestController
@RequestMapping(path = "/order")
public class OrderController {
	Logger logger = LoggerFactory.getLogger(getClass());

	private static final String SETTLE_ACCOUNT_CATEGORY = "settle-account-category";

	@Autowired
	private OrderService orderService;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private CodeStoreRepository codeStoreRepository;

	@RequestMapping(path = "/settleAccount")
	public ResultBean<String> settleAccount(@RequestBody SettleAccountDTO dto) throws Exception {
		OrderPreviewDTO previewDTO = orderService.settleAccount(dto);
		AssertUtil.assertTrue(!CollectionUtils.isEmpty(previewDTO.getItems()), "未选中商品");
		String key = UUID.randomUUID().toString();
		CodeStore store = new CodeStore();
		store.setCategory(SETTLE_ACCOUNT_CATEGORY);
		store.setKey(key);
		store.setValue(mapper.writeValueAsString(previewDTO));
		codeStoreRepository.save(store);
		return new ResultBean<>(key);
	}

	@RequestMapping(path = "/preview/params/{key}")
	public ResultBean<OrderPreviewDTO> getPreview(@PathVariable("key") String key) throws Exception {
		Optional<CodeStore> store = codeStoreRepository.findByCategoryAndKey(SETTLE_ACCOUNT_CATEGORY, key);
		OrderPreviewDTO dto = null;
		if (store.isPresent()) {
			dto = mapper.readValue(store.get().getValue(), OrderPreviewDTO.class);
		}
		return new ResultBean<>(dto);
	}

	@RequestMapping(path = "/create")
	public ResultBean<Long> create(@RequestBody SettleAccountDTO dto) {
		return new ResultBean<>(orderService.generateOrder(ClientContext.get(), dto));
	}

	@RequestMapping(path = "/list")
	public ResultBean<OrderDTO> list(@RequestBody QueryInfo<OrderStatus> queryInfo) {
		return new ResultBean<>(orderService.getOrders(ClientContext.get(), queryInfo).getList());
	}

	@RequestMapping(path = "/load/params/{id}")
	public ResultBean<OrderDTO> load(@PathVariable Long id) {
		return new ResultBean<>(orderService.getOrder(ClientContext.get(), id));
	}

	@RequestMapping(path = "/count")
	public ResultBean<Long> count(@RequestBody QueryInfo<OrderStatus> queryInfo) {
		return new ResultBean<>(orderService.getOrders(ClientContext.get(), queryInfo).getTotal());
	}

	@RequestMapping(path = "/count/uncomment")
	public ResultBean<Long> countUnComment() {
		return new ResultBean<>(orderService.countUncommentOrders(ClientContext.get()));
	}

	@RequestMapping(path = "/cancel/params/{id}")
	public ResultBean<Void> cancel(@PathVariable Long id) throws Exception {
		orderService.cancel(ClientContext.get(), id);
		return new ResultBean<>();
	}

	@RequestMapping(path = "/pickup/params/{id}")
	public ResultBean<Void> pickup(@PathVariable Long id) {
		orderService.pickup(ClientContext.get(), id);
		return new ResultBean<>();
	}

	@RequestMapping(path = "/delete/params/{id}")
	public ResultBean<Void> delete(@PathVariable Long id) {
		orderService.delete(ClientContext.get(), id);
		return new ResultBean<>();
	}

	@RequestMapping(path = "/checkPaid/params/{id}")
	public ResultBean<Boolean> checkPaid(@PathVariable Long id) {
		boolean paid = OrderStatus.Paid.equals(orderService.getOrder(ClientContext.get(), id).getStatus());
		return new ResultBean<>(paid);
	}

	@RequestMapping(path = "/coupon/available/params/{key}")
	public ResultBean<ClientCouponDTO> availableCoupons(@PathVariable("key") String key) throws Exception {
		Optional<CodeStore> store = codeStoreRepository.findByCategoryAndKey(SETTLE_ACCOUNT_CATEGORY, key);
		OrderPreviewDTO dto = null;
		if (store.isPresent()) {
			dto = mapper.readValue(store.get().getValue(), OrderPreviewDTO.class);
		}
		AssertUtil.assertNotNull(dto, "结算信息失效，请重新下单");
		return new ResultBean<>(orderService.getAvailableCouponListForSettleAccount(ClientContext.get(), dto));
	}
}
