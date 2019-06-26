package com.sourcecode.malls.service.impl;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.client.ClientCartItem;
import com.sourcecode.malls.domain.goods.GoodsItem;
import com.sourcecode.malls.domain.goods.GoodsItemProperty;
import com.sourcecode.malls.domain.goods.GoodsItemValue;
import com.sourcecode.malls.domain.order.Invoice;
import com.sourcecode.malls.domain.order.Order;
import com.sourcecode.malls.domain.order.OrderAddress;
import com.sourcecode.malls.domain.order.SubOrder;
import com.sourcecode.malls.dto.OrderItemDTO;
import com.sourcecode.malls.dto.OrderPreviewDTO;
import com.sourcecode.malls.dto.SettleAccountDTO;
import com.sourcecode.malls.dto.SettleItemDTO;
import com.sourcecode.malls.enums.OrderStatus;
import com.sourcecode.malls.enums.SubOrderStatus;
import com.sourcecode.malls.repository.jpa.impl.client.ClientCartRepository;
import com.sourcecode.malls.repository.jpa.impl.goods.GoodsItemPropertyRepository;
import com.sourcecode.malls.repository.jpa.impl.goods.GoodsItemRepository;
import com.sourcecode.malls.repository.jpa.impl.goods.GoodsItemValueRepository;
import com.sourcecode.malls.repository.jpa.impl.order.InvoiceRepository;
import com.sourcecode.malls.repository.jpa.impl.order.OrderAddressRepository;
import com.sourcecode.malls.repository.jpa.impl.order.OrderRepository;
import com.sourcecode.malls.repository.jpa.impl.order.SubOrderRepository;
import com.sourcecode.malls.service.FileOnlineSystemService;
import com.sourcecode.malls.util.AssertUtil;

@Service
@Transactional
public class OrderService {
	@Autowired
	private GoodsItemService itemService;

	@Autowired
	private ClientCartRepository cartRepository;

	@Autowired
	protected GoodsItemPropertyRepository propertyRepository;

	@Autowired
	protected GoodsItemValueRepository valueRepository;

	@Autowired
	protected OrderRepository orderRepository;

	@Autowired
	protected SubOrderRepository subOrderRepository;

	@Autowired
	protected FileOnlineSystemService fileService;

	@Autowired
	protected InvoiceRepository invoiceRepository;

	@Autowired
	protected OrderAddressRepository addressRepository;

	@Autowired
	private GoodsItemPropertyRepository goodsItemPropertyRepository;

	@Autowired
	private GoodsItemRepository goodsItemRepository;

	@Autowired
	protected EntityManager em;

	private String fileDir = "order";

	@Transactional(readOnly = true)
	public OrderPreviewDTO settleAccount(SettleAccountDTO dto) {
		AssertUtil.assertTrue(!CollectionUtils.isEmpty(dto.getItems()), "未选中商品");
		OrderPreviewDTO previewDTO = new OrderPreviewDTO();
		previewDTO.setFromCart(dto.isFromCart());
		List<OrderItemDTO> orderItems = new ArrayList<>();
		previewDTO.setItems(orderItems);
		for (SettleItemDTO itemDTO : dto.getItems()) {
			Optional<GoodsItem> goodsItem = goodsItemRepository.findById(itemDTO.getItemId());
			if (goodsItem.isPresent() && goodsItem.get().getMerchant().getId().equals(ClientContext.getMerchantId())) {
				if (goodsItem.get().isEnabled()) {
					Optional<GoodsItemProperty> property = goodsItemPropertyRepository
							.findById(itemDTO.getPropertyId());
					if (property.isPresent() && property.get().getItem().getId().equals(goodsItem.get().getId())) {
						OrderItemDTO orderItem = new OrderItemDTO();
						orderItem.setItem(goodsItem.get().asDTO(false, false));
						orderItem.setProperty(property.get().asDTO());
						orderItem.setNums(itemDTO.getNums());
						List<GoodsItemValue> values = valueRepository.findAllByUid(property.get().getUid());
						List<String> attrs = new ArrayList<>();
						for (GoodsItemValue value : values) {
							attrs.add(value.getValue().getName());
						}
						orderItem.setAttrs(attrs);
						orderItems.add(orderItem);
					}
				}
			}
		}
		return previewDTO;
	}

	public String generateOrderId() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		return sdf.format(new Date()) + new Random().nextInt(9999);
	}

	public void generateOrder(Client client, SettleAccountDTO dto) {
		AssertUtil.assertNotNull(dto.getAddress(), "收货地址不能为空");
		BigDecimal totalPrice = BigDecimal.ZERO;
		Order order = new Order();
		order.setClient(client);
		order.setMerchant(client.getMerchant());
		order.setOrderId(generateOrderId());
		order.setStatus(OrderStatus.UnPay);
		order.setPayment(dto.getPayment());
		order.setRemark(dto.getRemark());
		order.setTotalPrice(totalPrice);
		orderRepository.save(order);
		if (dto.getInvoice() != null) {
			Invoice invoice = dto.getInvoice().asEntity();
			invoice.setOrder(order);
			invoice.setClient(client);
			invoiceRepository.save(invoice);
		}
		OrderAddress address = dto.getAddress().asOrderAddressEntity();
		address.setOrder(order);
		addressRepository.save(address);
		List<SubOrder> subs = new ArrayList<>();
		if (dto.isFromCart()) {
			for (SettleItemDTO itemDTO : dto.getItems()) {
				Optional<ClientCartItem> cartItemOp = cartRepository.findById(itemDTO.getItemId());
				if (cartItemOp.isPresent() && cartItemOp.get().getClient().getId().equals(client.getId())) {
					ClientCartItem cartItem = cartItemOp.get();
					BigDecimal dealPrice = cartItem.getProperty().getPrice()
							.multiply(new BigDecimal(cartItem.getNums()));
					totalPrice = totalPrice.add(dealPrice);
					settleItem(client, cartItem.getItem(), cartItem.getProperty(), order, cartItem.getNums(), dealPrice,
							subs);
				}
			}
		} else {
			SettleItemDTO itemDTO = dto.getItems().get(0);
			Optional<GoodsItem> itemOp = itemService.findById(itemDTO.getItemId());
			if (itemOp.isPresent() && itemOp.get().getMerchant().getId().equals(client.getMerchant().getId())) {
				GoodsItem item = itemOp.get();
				Optional<GoodsItemProperty> propertyOp = propertyRepository.findById(itemDTO.getPropertyId());
				if (propertyOp.isPresent() && propertyOp.get().getItem().getId().equals(item.getId())) {
					GoodsItemProperty property = propertyOp.get();
					BigDecimal dealPrice = property.getPrice().multiply(new BigDecimal(itemDTO.getNums()));
					totalPrice = totalPrice.add(dealPrice);
					settleItem(client, item, property, order, itemDTO.getNums(), dealPrice, subs);
				}
			}
		}
		order.setTotalPrice(totalPrice);
		order.setSubList(subs);
		subOrderRepository.saveAll(subs);
		orderRepository.save(order);
	}

	private void settleItem(Client client, GoodsItem item, GoodsItemProperty property, Order parent, int nums,
			BigDecimal dealPrice, List<SubOrder> subs) {
		AssertUtil.assertTrue(item.isEnabled(), item.getName() + "已下架");
		SubOrder sub = new SubOrder();
		sub.setBrand(item.getBrand().getName());
		sub.setCategory(item.getCategory().getName());
		sub.setClient(client);
		sub.setDealPrice(dealPrice);
		sub.setItemCode(item.getCode());
		sub.setItemContent(item.getContent());
		sub.setItemId(item.getId());
		sub.setItemName(item.getName());
		sub.setMarketPrice(item.getMarketPrice());
		sub.setNums(nums);
		sub.setOrderId(generateOrderId());
		sub.setParent(parent);
		sub.setPayment(parent.getPayment());
		sub.setUnitPrice(property.getPrice());
		sub.setSellingPoints(item.getSellingPoints());
		sub.setStatus(SubOrderStatus.UnPay);
		List<GoodsItemValue> values = valueRepository.findAllByUid(property.getUid());
		StringBuilder spec = new StringBuilder();
		int index = 0;
		for (GoodsItemValue value : values) {
			spec.append(value.getValue());
			if (index < values.size() - 1) {
				spec.append(", ");
			}
			index++;
		}
		sub.setSpecificationValues(spec.toString());
		byte[] buf = fileService.load(true, item.getThumbnail());
		String filePath = fileDir + "/" + client.getMerchant().getId() + "/" + client.getId() + "/" + sub.getOrderId()
				+ "/thumb.png";
		fileService.upload(true, filePath, new ByteArrayInputStream(buf));
		sub.setThumbnail(filePath);
		em.lock(property, LockModeType.PESSIMISTIC_WRITE);
		int leftInventory = property.getInventory() - nums;
		AssertUtil.assertTrue(leftInventory >= 0, item.getName() + "库存不足");
		property.setInventory(leftInventory);
		subs.add(sub);
	}

	public void afterPayment(String orderId) {
		Optional<Order> orderOp = orderRepository.findByOrderId(orderId);
		if (orderOp.isPresent()) {
			Order order = orderOp.get();
			order.setStatus(OrderStatus.Paid);
			for (SubOrder subOrder : order.getSubList()) {
				subOrder.setStatus(SubOrderStatus.Paid);
			}
			subOrderRepository.saveAll(order.getSubList());
			orderRepository.save(order);
		}
	}

}
