package com.sourcecode.malls.web.controller;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.dto.base.KeyDTO;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.goods.GoodsItemDTO;
import com.sourcecode.malls.service.impl.GoodsItemService;

@RestController
@RequestMapping(path = "/client/cart")
public class ClientCartController {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private GoodsItemService itemService;

	@RequestMapping(path = "/list")
	public ResultBean<GoodsItemDTO> list(@RequestBody KeyDTO<Long> dto) {
		List<GoodsItemDTO> list = new ArrayList<>();
		for (Long id : dto.getIds()) {
			GoodsItemDTO item = itemService.load(ClientContext.getMerchantId(), id);
			list.add(item);
		}
		return new ResultBean<>(list);
	}
}
