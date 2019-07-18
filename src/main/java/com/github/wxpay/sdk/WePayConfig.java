package com.github.wxpay.sdk;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sourcecode.malls.dto.setting.DeveloperSettingDTO;

public class WePayConfig extends WXPayConfig implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String appId;
	private String key;
	private String mchId;
	private ByteArrayInputStream bis;

	public WePayConfig(DeveloperSettingDTO info, byte[] cert) {
		this.appId = info.getAccount();
		this.key = info.getSecret();
		this.mchId = info.getMch();
		this.bis = new ByteArrayInputStream(cert);
	}

	@Override
	public String getAppID() {
		return appId;
	}

	@Override
	public String getMchID() {
		return mchId;
	}

	@Override
	public String getKey() {
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
				if (ex != null) {
					logger.error("[WePay Error] - " + domain + ": " + ex.getMessage(), ex);
				}
			}

			@Override
			public DomainInfo getDomain(WXPayConfig config) {
				return new DomainInfo("api.mch.weixin.qq.com", true);
			}

		};
	}

}