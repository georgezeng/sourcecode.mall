package com.sourcecode.malls.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.github.wxpay.sdk.WePayConfig;
import com.sourcecode.malls.dto.setting.DeveloperSettingDTO;
import com.sourcecode.malls.util.AssertUtil;

@Component
public class WechatSettingService {
	@Autowired
	private MerchantSettingService service;

	@Cacheable(cacheNames = "wepay_config", key = "#merchantId")
	public WePayConfig createWePayConfig(Long merchantId) throws Exception {
		Optional<DeveloperSettingDTO> info = service.loadWechatGzh(merchantId);
		AssertUtil.assertTrue(info.isPresent(), "未找到商户的微信信息");
		return new WePayConfig(info.get(), service.loadWepayCert(merchantId));
	}
}
