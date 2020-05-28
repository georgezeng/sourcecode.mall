package com.sourcecode.malls.dto;

import java.math.BigDecimal;
import java.util.List;

public class OrderPreviewDTO {
	private List<OrderItemDTO> items;
	private boolean fromCart;
	private BigDecimal totalPrice;
	private BigDecimal realPrice;

	public BigDecimal getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(BigDecimal totalPrice) {
		this.totalPrice = totalPrice;
	}

	public BigDecimal getRealPrice() {
		return realPrice;
	}

	public void setRealPrice(BigDecimal realPrice) {
		this.realPrice = realPrice;
	}

	public boolean isFromCart() {
		return fromCart;
	}

	public void setFromCart(boolean fromCart) {
		this.fromCart = fromCart;
	}

	public List<OrderItemDTO> getItems() {
		return items;
	}

	public void setItems(List<OrderItemDTO> items) {
		this.items = items;
	}

}
