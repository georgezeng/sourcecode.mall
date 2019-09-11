package com.sourcecode.malls.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class ClientBonusInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private BigDecimal rookieCoupon;
	private BigDecimal inviteCoupon;
	private BigDecimal invitePoints;
	private BigDecimal rookiePoints;

	public BigDecimal getRookieCoupon() {
		return rookieCoupon;
	}

	public void setRookieCoupon(BigDecimal rookieCoupon) {
		this.rookieCoupon = rookieCoupon;
	}

	public BigDecimal getInviteCoupon() {
		return inviteCoupon;
	}

	public void setInviteCoupon(BigDecimal inviteCoupon) {
		this.inviteCoupon = inviteCoupon;
	}

	public BigDecimal getInvitePoints() {
		return invitePoints;
	}

	public void setInvitePoints(BigDecimal invitePoints) {
		this.invitePoints = invitePoints;
	}

	public BigDecimal getRookiePoints() {
		return rookiePoints;
	}

	public void setRookiePoints(BigDecimal rookiePoints) {
		this.rookiePoints = rookiePoints;
	}

}
