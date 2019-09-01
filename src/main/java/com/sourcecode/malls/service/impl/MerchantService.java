package com.sourcecode.malls.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;

import com.sourcecode.malls.domain.merchant.MerchantShopApplication;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantShopApplicationRepository;
import com.sourcecode.malls.util.AssertUtil;

public class MerchantService {
	@Autowired
	private MerchantShopApplicationRepository applicationRepository;

	@Cacheable(cacheNames = "merchant_shop_name", key = "#merchantId")
	public String getShopName(Long merchantId) {
		Optional<MerchantShopApplication> shop = applicationRepository.findByMerchantId(merchantId);
		AssertUtil.assertTrue(shop.isPresent(), "找不到商户信息");
		return shop.get().getName();
	}
}
