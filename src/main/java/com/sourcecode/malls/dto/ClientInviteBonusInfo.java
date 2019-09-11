package com.sourcecode.malls.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class ClientInviteBonusInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private BigDecimal coupon;
	private BigDecimal points;

	public BigDecimal getCoupon() {
		return coupon;
	}

	public void setCoupon(BigDecimal coupon) {
		this.coupon = coupon;
	}

	public BigDecimal getPoints() {
		return points;
	}

	public void setPoints(BigDecimal points) {
		this.points = points;
	}
}
