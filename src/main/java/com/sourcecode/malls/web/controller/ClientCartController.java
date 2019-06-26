package com.sourcecode.malls.web.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sourcecode.malls.constants.ExceptionMessageConstant;
import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.client.ClientCartItem;
import com.sourcecode.malls.domain.goods.GoodsItem;
import com.sourcecode.malls.dto.base.KeyDTO;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.client.ClientCartItemDTO;
import com.sourcecode.malls.repository.jpa.impl.client.ClientCartRepository;
import com.sourcecode.malls.repository.jpa.impl.goods.GoodsItemRepository;
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
	private GoodsItemRepository itemRepository;

	@RequestMapping(path = "/item/params/{id}")
	public ResultBean<Map<String, Integer>> itemInfo(@PathVariable Long id) {
		int total = cartRepository.findByClient(ClientContext.get()).size();
		Map<String, Integer> map = new HashMap<>();
		map.put("total", total);
		if (id > 0) {
			Optional<GoodsItem> item = itemRepository.findById(id);
			AssertUtil.assertTrue(
					item.isPresent() && item.get().getMerchant().getId().equals(ClientContext.getMerchantId()),
					ExceptionMessageConstant.NO_SUCH_RECORD);
			List<ClientCartItem> cart = cartRepository.findByClientAndItem(ClientContext.get(), item.get());
			total = 0;
			for (ClientCartItem cartItem : cart) {
				total += cartItem.getNums();
			}
			map.put("itemNums", total);
		}
		return new ResultBean<>(map);
	}

	@RequestMapping(path = "/list")
	public ResultBean<ClientCartItemDTO> list(@RequestBody KeyDTO<Long> dto) {
		return new ResultBean<>(clientCartService.getCart(ClientContext.get()));
	}

	@RequestMapping(path = "/save")
	public ResultBean<Map<String, Integer>> save(@RequestBody ClientCartItemDTO dto) {
		clientCartService.saveCart(ClientContext.get(), dto);
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
		return new ResultBean<>();
	}

	@RequestMapping(path = "/delete/params/{id}")
	public ResultBean<Void> delete(@PathVariable Long id) {
		cartRepository.deleteById(id);
		return new ResultBean<>();
	}
	
	@RequestMapping(path = "/settleAccount")
	public ResultBean<Void> settleAccount() {
		return new ResultBean<>();
	}
}
