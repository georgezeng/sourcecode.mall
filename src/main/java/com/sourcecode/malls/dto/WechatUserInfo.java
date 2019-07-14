package com.sourcecode.malls.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sourcecode.malls.dto.wechat.WechatBaseInfo;

public class WechatUserInfo extends WechatBaseInfo {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String accessToken;
	private String refreshToken;
	@JsonProperty("openid")
	private String openId;
	private String nickname;
	@JsonProperty("headimgurl")
	private String headImgUrl;
	@JsonProperty("unionid")
	private String unionId;
	private int sex;

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public int getSex() {
		return sex;
	}

	public void setSex(int sex) {
		this.sex = sex;
	}

	public String getUnionId() {
		return unionId;
	}

	public void setUnionId(String unionId) {
		this.unionId = unionId;
	}

	public String getOpenId() {
		return openId;
	}

	public void setOpenId(String openId) {
		this.openId = openId;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getHeadImgUrl() {
		return headImgUrl;
	}

	public void setHeadImgUrl(String headImgUrl) {
		this.headImgUrl = headImgUrl;
	}
}