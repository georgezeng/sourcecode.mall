package com.sourcecode.malls.web.controller;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.goods.GoodsItem;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.goods.GoodsItemDTO;
import com.sourcecode.malls.dto.query.PageInfo;
import com.sourcecode.malls.service.impl.GoodsItemService;

@RestController
@RequestMapping(path = "/goods/item")
public class GoodsItemController {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private GoodsItemService service;

	@RequestMapping(path = "/list/params/{id}/{type}")
	public ResultBean<GoodsItemDTO> list(@PathVariable("id") Long categoryId, @PathVariable("type") String type,
			@RequestBody PageInfo pageInfo) {
		Page<GoodsItem> result = service.findByCategory(ClientContext.getMerchantId(), categoryId, type, pageInfo);
		return new ResultBean<>(result.getContent().stream().map(it -> it.asDTO()).collect(Collectors.toList()));
	}

}
