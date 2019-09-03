package com.sourcecode.malls.web.controller;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sourcecode.malls.constants.ExceptionMessageConstant;
import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.client.ClientCartItem;
import com.sourcecode.malls.dto.base.KeyDTO;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.client.ClientCartItemDTO;
import com.sourcecode.malls.repository.jpa.impl.client.ClientCartRepository;
import com.sourcecode.malls.service.impl.CacheEvictService;
import com.sourcecode.malls.service.impl.ClientCartService;
import com.sourcecode.malls.util.AssertUtil;

@RestController
@RequestMapping(path = "/client/cart")
public class ClientCartController {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ClientCartService clientCartService;

	@Autowired
	private ClientCartRepository cartRepository;

	@Autowired
	private CacheEvictService evictService;

	@RequestMapping(path = "/item/params/{id}")
	public ResultBean<Map<String, Long>> itemInfo(@PathVariable Long id) {
		return new ResultBean<>(clientCartService.calItemInfo(ClientContext.get(), id));
	}

	@RequestMapping(path = "/list")
	public ResultBean<ClientCartItemDTO> list() {
		return new ResultBean<>(clientCartService.getCart(ClientContext.get()));
	}

	@RequestMapping(path = "/save")
	public ResultBean<Map<String, Long>> save(@RequestBody ClientCartItemDTO dto) {
		clientCartService.saveCart(ClientContext.get(), dto);
		evictService.clearClientCartItems(ClientContext.get().getId());
		return itemInfo(dto.getItemId());
	}

	@RequestMapping(path = "/updateNums/params/{id}/{nums}")
	public ResultBean<Void> updateNums(@PathVariable("id") Long id, @PathVariable("nums") Integer nums) {
		Optional<ClientCartItem> cartItem = cartRepository.findById(id);
		AssertUtil.assertTrue(
				cartItem.isPresent() && cartItem.get().getClient().getId().equals(ClientContext.get().getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		cartItem.get().setNums(nums);
		cartRepository.save(cartItem.get());
		evictService.clearClientCartItems(ClientContext.get().getId());
		return new ResultBean<>();
	}

	@RequestMapping(path = "/delete")
	public ResultBean<Void> delete(@RequestBody KeyDTO<Long> keys) {
		AssertUtil.assertTrue(!CollectionUtils.isEmpty(keys.getIds()), "至少选中一个商品");
		for (Long id : keys.getIds()) {
			cartRepository.deleteById(id);
		}
		evictService.clearClientCartItems(ClientContext.get().getId());
		return new ResultBean<>();
	}

}
