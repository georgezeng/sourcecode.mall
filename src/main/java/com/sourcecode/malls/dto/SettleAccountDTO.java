package com.sourcecode.malls.dto;

import java.util.List;

import com.sourcecode.malls.dto.client.ClientAddressDTO;
import com.sourcecode.malls.dto.order.InvoiceDTO;
import com.sourcecode.malls.enums.Payment;

public class SettleAccountDTO {
	private ClientAddressDTO address;
	private List<SettleItemDTO> items;
	private Payment payment;
	private boolean fromCart;
	private InvoiceDTO invoice;
	private String remark;
	private List<ClientCouponDTO> coupons;

	public List<ClientCouponDTO> getCoupons() {
		return coupons;
	}

	public void setCoupons(List<ClientCouponDTO> coupons) {
		this.coupons = coupons;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public InvoiceDTO getInvoice() {
		return invoice;
	}

	public void setInvoice(InvoiceDTO invoice) {
		this.invoice = invoice;
	}

	public boolean isFromCart() {
		return fromCart;
	}

	public void setFromCart(boolean fromCart) {
		this.fromCart = fromCart;
	}

	public ClientAddressDTO getAddress() {
		return address;
	}

	public void setAddress(ClientAddressDTO address) {
		this.address = address;
	}

	public List<SettleItemDTO> getItems() {
		return items;
	}

	public void setItems(List<SettleItemDTO> items) {
		this.items = items;
	}

	public Payment getPayment() {
		return payment;
	}

	public void setPayment(Payment payment) {
		this.payment = payment;
	}
}
