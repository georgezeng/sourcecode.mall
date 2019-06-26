package com.sourcecode.malls.dto;

import java.util.List;

import com.sourcecode.malls.dto.goods.GoodsItemDTO;
import com.sourcecode.malls.dto.goods.GoodsItemPropertyDTO;

public class OrderItemDTO {
	private GoodsItemDTO item;
	private GoodsItemPropertyDTO property;
	private List<String> attrs;
	private int nums;

	public List<String> getAttrs() {
		return attrs;
	}

	public void setAttrs(List<String> attrs) {
		this.attrs = attrs;
	}

	public GoodsItemDTO getItem() {
		return item;
	}

	public void setItem(GoodsItemDTO item) {
		this.item = item;
	}

	public GoodsItemPropertyDTO getProperty() {
		return property;
	}

	public void setProperty(GoodsItemPropertyDTO property) {
		this.property = property;
	}

	public int getNums() {
		return nums;
	}

	public void setNums(int nums) {
		this.nums = nums;
	}

}
