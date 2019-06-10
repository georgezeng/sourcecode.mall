package com.sourcecode.malls.web.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.goods.GoodsCategory;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.goods.GoodsAttributeDTO;
import com.sourcecode.malls.repository.jpa.impl.goods.GoodsCategoryRepository;
import com.sourcecode.malls.util.AssertUtil;

@RestController
@RequestMapping(path = "/goods/category")
public class GoodsCategoryController {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private GoodsCategoryRepository repository;

	@RequestMapping(path = "/list/level1")
	public ResultBean<GoodsAttributeDTO> listLevel1() {
		List<GoodsCategory> list = repository.findByMerchantAndParentIsNull(ClientContext.get().getMerchant());
		return new ResultBean<>(list.stream().map(it -> it.asDTO()).collect(Collectors.toList()));
	}

	@RequestMapping(path = "/list/level2/params/{id}")
	public ResultBean<GoodsAttributeDTO> listLevel2(@PathVariable Long id) {
		Optional<GoodsCategory> parent = repository.findById(id);
		AssertUtil.assertTrue(parent.isPresent(), "找不到商品分类");
		List<GoodsCategory> list = repository.findByMerchantAndParent(ClientContext.get().getMerchant(), parent.get());
		return new ResultBean<>(list.stream().map(it -> it.asDTO(false, true)).collect(Collectors.toList()));
	}

}
