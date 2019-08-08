package com.sourcecode.malls.web.controller;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.merchant.MerchantShopApplication;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantShopApplicationRepository;
import com.sourcecode.malls.util.AssertUtil;

@RestController
@RequestMapping(path = "/merchant")
public class MerchantController {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private MerchantShopApplicationRepository applicationRepository;

	@RequestMapping(path = "/shopName")
	public ResultBean<String> shopName() {
		Optional<MerchantShopApplication> shop = applicationRepository.findByMerchantId(ClientContext.getMerchantId());
		AssertUtil.assertTrue(shop.isPresent(), "找不到商户信息");
		return new ResultBean<>(shop.get().getName());
	}

}
