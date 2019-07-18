package com.sourcecode.malls.service.impl;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WePayConfig;
import com.sourcecode.malls.constants.EnvConstant;
import com.sourcecode.malls.constants.ExceptionMessageConstant;
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
import com.sourcecode.malls.dto.order.OrderDTO;
import com.sourcecode.malls.dto.query.PageInfo;
import com.sourcecode.malls.dto.query.PageResult;
import com.sourcecode.malls.dto.query.QueryInfo;
import com.sourcecode.malls.enums.OrderStatus;
import com.sourcecode.malls.enums.Payment;
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

	@Autowired
	private WechatSettingService wechatSettingService;

	private String fileDir = "order";

	@Value("${wechat.api.url.refund.notify}")
	private String refundNotifyUrl;

	@Autowired
	private Environment env;

	@Transactional(readOnly = true)
	public OrderPreviewDTO settleAccount(SettleAccountDTO dto) {
		AssertUtil.assertTrue(!CollectionUtils.isEmpty(dto.getItems()), "未选中商品");
		OrderPreviewDTO previewDTO = new OrderPreviewDTO();
		previewDTO.setFromCart(dto.isFromCart());
		List<OrderItemDTO> orderItems = new ArrayList<>();
		previewDTO.setItems(orderItems);
		for (SettleItemDTO itemDTO : dto.getItems()) {
			Optional<GoodsItem> goodsItem = Optional.empty();
			if (previewDTO.isFromCart()) {
				Optional<ClientCartItem> cartItem = cartRepository.findById(itemDTO.getItemId());
				if (cartItem.isPresent()) {
					goodsItem = Optional.of(cartItem.get().getItem());
				}
			} else {
				goodsItem = goodsItemRepository.findById(itemDTO.getItemId());
			}
			if (goodsItem.isPresent() && goodsItem.get().getMerchant().getId().equals(ClientContext.getMerchantId())) {
				if (goodsItem.get().isEnabled()) {
					Optional<GoodsItemProperty> property = goodsItemPropertyRepository
							.findById(itemDTO.getPropertyId());
					if (property.isPresent() && property.get().getItem().getId().equals(goodsItem.get().getId())) {
						OrderItemDTO orderItem = new OrderItemDTO();
						if (previewDTO.isFromCart()) {
							orderItem.setCartItemId(itemDTO.getItemId());
						}
						orderItem.setItem(goodsItem.get().asDTO(false, false, false));
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

	public Long generateOrder(Client client, SettleAccountDTO dto) {
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
				Optional<ClientCartItem> cartItemOp = cartRepository.findById(itemDTO.getCartItemId());
				if (cartItemOp.isPresent() && cartItemOp.get().getClient().getId().equals(client.getId())) {
					ClientCartItem cartItem = cartItemOp.get();
					BigDecimal dealPrice = cartItem.getProperty().getPrice()
							.multiply(new BigDecimal(cartItem.getNums()));
					totalPrice = totalPrice.add(dealPrice);
					settleItem(client, cartItem.getItem(), cartItem.getProperty(), order, cartItem.getNums(), dealPrice,
							subs);
					cartRepository.delete(cartItem);
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
		return order.getId();
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
		sub.setParent(parent);
		sub.setUnitPrice(property.getPrice());
		sub.setSellingPoints(item.getSellingPoints());
		List<GoodsItemValue> values = valueRepository.findAllByUid(property.getUid());
		StringBuilder spec = new StringBuilder();
		int index = 0;
		for (GoodsItemValue value : values) {
			spec.append(value.getValue().getName());
			if (index < values.size() - 1) {
				spec.append(", ");
			}
			index++;
		}
		sub.setSpecificationValues(spec.toString());
		byte[] buf = fileService.load(true, item.getThumbnail());
		String filePath = fileDir + "/" + client.getMerchant().getId() + "/" + client.getId() + "/" + sub.getId()
				+ "/thumb.png";
		fileService.upload(true, filePath, new ByteArrayInputStream(buf));
		sub.setThumbnail(filePath);
		em.lock(property, LockModeType.PESSIMISTIC_WRITE);
		int leftInventory = property.getInventory() - nums;
		AssertUtil.assertTrue(leftInventory >= 0, item.getName() + "库存不足");
		property.setInventory(leftInventory);
		sub.setProperty(property);
		subs.add(sub);
	}

	public void afterPayment(String orderId, String transactionId) {
		Optional<Order> orderOp = orderRepository.findByOrderId(orderId);
		if (orderOp.isPresent() && OrderStatus.UnPay.equals(orderOp.get().getStatus())) {
			Order order = orderOp.get();
			em.lock(order, LockModeType.PESSIMISTIC_WRITE);
			order.setStatus(OrderStatus.Paid);
			order.setPayTime(new Date());
			order.setTransactionId(transactionId);
			orderRepository.save(order);
		}
	}

	@Transactional(readOnly = true)
	public PageResult<OrderDTO> getOrders(Client client, QueryInfo<OrderStatus> queryInfo) {
		Specification<Order> spec = new Specification<Order>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Predicate toPredicate(Root<Order> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
				List<Predicate> predicate = new ArrayList<>();
				predicate.add(criteriaBuilder.equal(root.get("client"), client));
				predicate.add(criteriaBuilder.equal(root.get("deleted"), false));
				if (queryInfo.getData() != null) {
					predicate.add(criteriaBuilder.equal(root.get("status"), queryInfo.getData()));
				}
				return query.where(predicate.toArray(new Predicate[] {})).getRestriction();
			}
		};
		Page<Order> orders = orderRepository.findAll(spec, queryInfo.getPage().pageable(Direction.DESC, "createTime"));
		return new PageResult<>(orders.get().map(order -> order.asDTO(true, false)).collect(Collectors.toList()),
				orders.getTotalElements());
	}

	@Transactional(readOnly = true)
	public OrderDTO getOrder(Client client, Long id) {
		Optional<Order> order = orderRepository.findById(id);
		AssertUtil.assertTrue(order.isPresent() && order.get().getClient().getId().equals(client.getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		return order.get().asDTO(true, true);
	}

	@Transactional(readOnly = true)
	public Long countUncommentOrders(Client client) {
		Specification<SubOrder> spec = new Specification<SubOrder>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Predicate toPredicate(Root<SubOrder> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
				List<Predicate> predicate = new ArrayList<>();
				Join<SubOrder, Order> join = root.join("parent");
				predicate.add(criteriaBuilder.equal(root.get("client"), client));
				predicate.add(criteriaBuilder.equal(root.get("comment"), false));
				predicate.add(criteriaBuilder.equal(join.get("deleted"), false));
				predicate.add(criteriaBuilder.equal(join.get("status"), OrderStatus.Finished));
				return query.where(predicate.toArray(new Predicate[] {})).getRestriction();
			}
		};
		PageInfo page = new PageInfo();
		page.setNum(1);
		page.setSize(1);
		Page<SubOrder> orders = subOrderRepository.findAll(spec, page.pageable());
		return orders.getTotalElements();
	}

	public void cancel(Client client, Long id) throws Exception {
		Optional<Order> orderOp = orderRepository.findById(id);
		AssertUtil.assertTrue(orderOp.isPresent() && orderOp.get().getClient().getId().equals(client.getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		Order order = orderOp.get();
		OrderStatus status = order.getStatus();
		boolean paid = OrderStatus.Paid.equals(status);
		AssertUtil.assertTrue(OrderStatus.UnPay.equals(status) || paid, "不能取消订单");
		if (paid) {
			// 自动退款
			if (Payment.WePay.equals(order.getPayment())) {
				WePayConfig config = wechatSettingService.createWePayConfig(client.getMerchant().getId());
				WXPay wxpay = new WXPay(config);
				Map<String, String> data = new HashMap<String, String>();
				data.put("transaction_id", order.getTransactionId());
				data.put("out_refund_no", order.getOrderId());
				String fee = order.getTotalPrice().multiply(new BigDecimal("100")).intValue() + "";
				if (!env.acceptsProfiles(Profiles.of(EnvConstant.PROD))) {
					fee = "1";
				}
				data.put("total_fee", fee);
				data.put("refund_fee", fee);
				data.put("notify_url", refundNotifyUrl);
				Map<String, String> resp = wxpay.refund(data);
				AssertUtil.assertTrue("SUCCESS".equals(resp.get("return_code")), "支付失败: " + resp.get("return_msg"));
				AssertUtil.assertTrue("SUCCESS".equals(resp.get("result_code")), "支付失败: " + resp.get("err_code_des"));
			}
		} else {
			afterCancel(order.getOrderId());
		}
	}

	public void afterCancel(String orderId) {
		Optional<Order> orderOp = orderRepository.findByOrderId(orderId);
		if (orderOp.isPresent() && (OrderStatus.UnPay.equals(orderOp.get().getStatus())
				|| OrderStatus.Paid.equals(orderOp.get().getStatus()))) {
			Order order = orderOp.get();
			em.lock(order, LockModeType.PESSIMISTIC_WRITE);
			order.setStatus(OrderStatus.Canceled);
			orderRepository.save(order);
			List<SubOrder> list = order.getSubList();
			if (list != null) {
				for (SubOrder sub : list) {
					GoodsItemProperty property = sub.getProperty();
					if (property != null) {
						em.lock(property, LockModeType.PESSIMISTIC_WRITE);
						property.setInventory(property.getInventory() + sub.getNums());
						goodsItemPropertyRepository.save(property);
					}
				}
			}
		}
	}

	public void pickup(Client client, Long id) {
		Optional<Order> order = orderRepository.findById(id);
		AssertUtil.assertTrue(order.isPresent() && order.get().getClient().getId().equals(client.getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		AssertUtil.assertTrue(OrderStatus.Shipped.equals(order.get().getStatus()), "不能确认收货，订单状态有误");
		em.lock(order.get(), LockModeType.PESSIMISTIC_WRITE);
		order.get().setStatus(OrderStatus.Finished);
		orderRepository.save(order.get());
	}

	public void delete(Client client, Long id) {
		Optional<Order> order = orderRepository.findById(id);
		AssertUtil.assertTrue(order.isPresent() && order.get().getClient().getId().equals(client.getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		OrderStatus status = order.get().getStatus();
		AssertUtil.assertTrue(OrderStatus.Canceled.equals(status) || OrderStatus.Closed.equals(status)
				|| OrderStatus.Finished.equals(status), "不能清除订单，订单状态有误");
		order.get().setDeleted(true);
		orderRepository.save(order.get());
	}

}
