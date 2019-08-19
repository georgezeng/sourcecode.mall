package com.sourcecode.malls.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.sourcecode.malls.dto.goods.GoodsAttributeDTO;
import com.sourcecode.malls.dto.goods.GoodsItemDTO;
import com.sourcecode.malls.enums.ClientCouponType;
import com.sourcecode.malls.enums.CouponRelationType;

public class ClientCouponDTO {
	private Long id;
	private ClientCouponType type;
	private String title;
	private BigDecimal amount;
	private CouponRelationType hxType;
	private Date startDate;
	private Date endDate;
	private String description;
	private List<GoodsAttributeDTO> categories;
	private List<GoodsItemDTO> items;

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

	public ClientCouponType getType() {
		return type;
	}

	public void setType(ClientCouponType type) {
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
