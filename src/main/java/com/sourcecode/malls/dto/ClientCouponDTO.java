package com.sourcecode.malls.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sourcecode.malls.dto.goods.GoodsAttributeDTO;
import com.sourcecode.malls.dto.goods.GoodsItemDTO;
import com.sourcecode.malls.enums.CouponType;
import com.sourcecode.malls.enums.CouponEventType;
import com.sourcecode.malls.enums.CouponRelationType;

public class ClientCouponDTO {
	private Long id;
	private CouponType type;
	private String title;
	private BigDecimal amount;
	private CouponRelationType hxType;
	private CouponEventType eventType;
	@JsonFormat(pattern = "yyyy.MM.dd", timezone = "GMT+8")
	private Date startDate;
	@JsonFormat(pattern = "yyyy.MM.dd", timezone = "GMT+8")
	private Date endDate;
	private String description;
	private List<GoodsAttributeDTO> categories;
	private List<GoodsItemDTO> items;
	private int limitedNums;
	private String imgPath;
	private String couponId;
	private String fromOrderId;
	private String invitee;

	public CouponEventType getEventType() {
		return eventType;
	}

	public void setEventType(CouponEventType eventType) {
		this.eventType = eventType;
	}

	public String getInvitee() {
		return invitee;
	}

	public void setInvitee(String invitee) {
		this.invitee = invitee;
	}

	public String getCouponId() {
		return couponId;
	}

	public void setCouponId(String couponId) {
		this.couponId = couponId;
	}

	public String getFromOrderId() {
		return fromOrderId;
	}

	public void setFromOrderId(String fromOrderId) {
		this.fromOrderId = fromOrderId;
	}

	public String getImgPath() {
		return imgPath;
	}

	public void setImgPath(String imgPath) {
		this.imgPath = imgPath;
	}

	public int getLimitedNums() {
		return limitedNums;
	}

	public void setLimitedNums(int limitedNums) {
		this.limitedNums = limitedNums;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public CouponType getType() {
		return type;
	}

	public void setType(CouponType type) {
		this.type = type;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public CouponRelationType getHxType() {
		return hxType;
	}

	public void setHxType(CouponRelationType hxType) {
		this.hxType = hxType;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<GoodsAttributeDTO> getCategories() {
		return categories;
	}

	public void setCategories(List<GoodsAttributeDTO> categories) {
		this.categories = categories;
	}

	public List<GoodsItemDTO> getItems() {
		return items;
	}

	public void setItems(List<GoodsItemDTO> items) {
		this.items = items;
	}
}
