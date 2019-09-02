package com.sourcecode.malls.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sourcecode.malls.constants.CacheNameConstant;
import com.sourcecode.malls.dto.merchant.SiteInfo;

@Service
public class MerchantService {
	@Autowired
	private MerchantSettingService settingService;

	@Cacheable(cacheNames = CacheNameConstant.MERCHANT_SITE_INFO, key = "#merchantId")
	public SiteInfo getSiteInfo(Long merchantId) throws Exception {
		return settingService.loadSiteInfo(merchantId);
	}
}
