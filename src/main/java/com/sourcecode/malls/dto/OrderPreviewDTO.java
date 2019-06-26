package com.sourcecode.malls.dto;

import java.util.List;

public class OrderPreviewDTO {
	private List<OrderItemDTO> items;
	private boolean fromCart;

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
