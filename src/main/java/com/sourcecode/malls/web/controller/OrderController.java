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
import com.sourcecode.malls.dto.OrderPreviewDTO;
import com.sourcecode.malls.dto.SettleAccountDTO;
import com.sourcecode.malls.dto.base.ResultBean;
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
	public ResultBean<Void> create(@RequestBody SettleAccountDTO dto) throws Exception {
		orderService.generateOrder(ClientContext.get(), dto);
		return new ResultBean<>();
	}

}
