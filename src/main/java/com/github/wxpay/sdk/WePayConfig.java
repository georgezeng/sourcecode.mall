package com.github.wxpay.sdk;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sourcecode.malls.dto.setting.DeveloperSettingDTO;

public class WePayConfig extends WXPayConfig {

	private String appId;
	private String key;
	private String mchId;
	private ByteArrayInputStream bis;
	private String domain;

	public WePayConfig(DeveloperSettingDTO info, byte[] cert, String domain) {
		this.appId = info.getAccount();
		this.key = info.getSecret();
		this.mchId = info.getMch();
		this.bis = new ByteArrayInputStream(cert);
		this.domain = domain;
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
		return new IWXPayDomain() {
			private Logger logger = LoggerFactory.getLogger(getClass());

			@Override
			public void report(String domain, long elapsedTimeMillis, Exception ex) {
				logger.error("[WePay Error] - " + domain + ": " + ex.getMessage(), ex);
			}

			@Override
			public DomainInfo getDomain(WXPayConfig config) {
				return new DomainInfo(domain, true);
			}

		};
	}

}