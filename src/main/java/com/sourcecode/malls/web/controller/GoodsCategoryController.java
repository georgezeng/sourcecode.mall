package com.sourcecode.malls.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.goods.GoodsAttributeDTO;
import com.sourcecode.malls.dto.goods.GoodsRecommendCategoryDTO;
import com.sourcecode.malls.service.impl.GoodsItemService;

@RestController
@RequestMapping(path = "/goods/category")
public class GoodsCategoryController {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private GoodsItemService service;

	@RequestMapping(path = "/list/level1")
	public ResultBean<GoodsAttributeDTO> listLevel1() {
		return new ResultBean<>(service.listCategoryLevel1(ClientContext.getMerchantId()));
	}

	@RequestMapping(path = "/list/level2/params/{id}")
	public ResultBean<GoodsAttributeDTO> listLevel2(@PathVariable Long id) {
		return new ResultBean<>(service.listCategoryLevel2(id));
	}
	
	@RequestMapping(path = "/recommend/list/params/{id}")
	public ResultBean<GoodsRecommendCategoryDTO> listRecommend(@PathVariable Long id) {
		return new ResultBean<>(service.listRecommendCategory(id));
	}

}
