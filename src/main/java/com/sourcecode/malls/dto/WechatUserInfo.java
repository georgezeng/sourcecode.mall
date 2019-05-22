package com.sourcecode.malls.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WechatUserInfo {
	@JsonProperty("openid")
	private String openId;
	private String nickname;
	@JsonProperty("headimgurl")
	private String headImgUrl;
	@JsonProperty("unionid")
	private String unionId;

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