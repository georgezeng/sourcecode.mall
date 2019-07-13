package com.github.wxpay.sdk;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.sourcecode.malls.dto.setting.DeveloperSettingDTO;

public class WePayConfig extends WXPayConfig {
	private String appId;
	private String key;
	private String mchId;
	private ByteArrayInputStream bis;

	public WePayConfig(DeveloperSettingDTO info) {
		this.appId = info.getAccount();
		this.key = info.getSecret();
		this.mchId = info.getMch();
		this.bis = new ByteArrayInputStream(info.getCert());
	}

	@Override
	String getAppID() {
		return appId;
	}

	@Override
	String getMchID() {
		return mchId;
	}

	@Override
	String getKey() {
		return key;
	}

	@Override
	InputStream getCertStream() {
		return bis;
	}

	@Override
	IWXPayDomain getWXPayDomain() {
		return null;
	}

}