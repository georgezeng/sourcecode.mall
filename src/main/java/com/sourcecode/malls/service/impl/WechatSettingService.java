package com.sourcecode.malls.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.wxpay.sdk.WePayConfig;
import com.sourcecode.malls.dto.setting.DeveloperSettingDTO;
import com.sourcecode.malls.util.AssertUtil;

@Component
public class WechatSettingService {
	@Autowired
	private MerchantSettingService service;

	public WePayConfig createWePayConfig(Long merchantId) throws Exception {
		Optional<DeveloperSettingDTO> info = service.loadWechatGzh(merchantId);
		AssertUtil.assertTrue(info.isPresent(), "未找到商家的微信信息");
		DeveloperSettingDTO dto = info.get();
		dto.setCert(service.loadWepayCert(merchantId));
		return new WePayConfig(dto);
	}
}
