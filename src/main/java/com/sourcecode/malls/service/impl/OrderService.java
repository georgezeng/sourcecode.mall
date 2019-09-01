package com.sourcecode.malls.service.impl;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.sourcecode.malls.constants.CacheNameConstant;
import com.sourcecode.malls.constants.EnvConstant;
import com.sourcecode.malls.constants.ExceptionMessageConstant;
import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.aftersale.AfterSaleApplication;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.client.ClientCartItem;
import com.sourcecode.malls.domain.coupon.ClientCoupon;
import com.sourcecode.malls.domain.coupon.cash.CashCouponOrderLimitedSetting;
import com.sourcecode.malls.domain.goods.GoodsItem;
import com.sourcecode.malls.domain.goods.GoodsItemProperty;
import com.sourcecode.malls.domain.goods.GoodsItemRank;
import com.sourcecode.malls.domain.goods.GoodsSpecificationValue;
import com.sourcecode.malls.domain.merchant.Merchant;
import com.sourcecode.malls.domain.order.Invoice;
import com.sourcecode.malls.domain.order.Order;
import com.sourcecode.malls.domain.order.OrderAddress;
import com.sourcecode.malls.domain.order.SubOrder;
import com.sourcecode.malls.dto.ClientCouponDTO;
import com.sourcecode.malls.dto.OrderItemDTO;
import com.sourcecode.malls.dto.OrderPreviewDTO;
import com.sourcecode.malls.dto.SettleAccountDTO;
import com.sourcecode.malls.dto.SettleItemDTO;
import com.sourcecode.malls.dto.order.OrderDTO;
import com.sourcecode.malls.dto.query.PageResult;
import com.sourcecode.malls.dto.query.QueryInfo;
import com.sourcecode.malls.enums.AfterSaleStatus;
import com.sourcecode.malls.enums.ClientCouponStatus;
import com.sourcecode.malls.enums.OrderStatus;
import com.sourcecode.malls.repository.jpa.impl.aftersale.AfterSaleApplicationRepository;
import com.sourcecode.malls.repository.jpa.impl.client.ClientCartRepository;
import com.sourcecode.malls.repository.jpa.impl.coupon.CashCouponOrderLimitedSettingRepository;
import com.sourcecode.malls.repository.jpa.impl.coupon.ClientCouponRepository;
import com.sourcecode.malls.repository.jpa.impl.coupon.CouponSettingRepository;
import com.sourcecode.malls.repository.jpa.impl.goods.GoodsItemPropertyRepository;
import com.sourcecode.malls.repository.jpa.impl.goods.GoodsItemRankRepository;
import com.sourcecode.malls.repository.jpa.impl.goods.GoodsItemRepository;
import com.sourcecode.malls.repository.jpa.impl.order.InvoiceRepository;
import com.sourcecode.malls.repository.jpa.impl.order.OrderAddressRepository;
import com.sourcecode.malls.repository.jpa.impl.order.OrderRepository;
import com.sourcecode.malls.repository.jpa.impl.order.SubOrderRepository;
import com.sourcecode.malls.service.FileOnlineSystemService;
import com.sourcecode.malls.service.base.BaseService;
import com.sourcecode.malls.util.AssertUtil;

@Service
@Transactional
public class OrderService implements BaseService {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private GoodsItemService itemService;

	@Autowired
	private ClientCartRepository cartRepository;

	@Autowired
	protected GoodsItemPropertyRepository propertyRepository;

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
	private GoodsItemRankRepository rankRepository;

	@Autowired
	protected EntityManager em;

//	@Autowired
//	private WechatService wechatService;
//
//	@Autowired
//	private AlipayService alipayService;

	@Autowired
	private ClientService clientService;

	@Autowired
	private AfterSaleApplicationRepository aftersaleApplicationRepository;

	@Autowired
	private CouponSettingRepository couponSettingRepository;

	@Autowired
	private ClientCouponRepository clientCouponRepository;

	@Autowired
	private CashCouponOrderLimitedSettingRepository cashCouponOrderLimitedSettingRepository;

	@Value("${user.type.name}")
	private String userDir;

	@Autowired
	private CacheEvictService cacheEvictService;

	private String fileDir = "subOrder";

	@Autowired
	private Environment env;

	@Autowired
	private ClientBonusService bonusService;

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
						List<String> attrs = new ArrayList<>();
						for (GoodsSpecificationValue value : property.get().getValues()) {
							attrs.add(value.getName());
						}
						orderItem.setAttrs(attrs);
						orderItems.add(orderItem);
					}
				}
			}
		}
		return previewDTO;
	}

	public Long generateOrder(Client client, SettleAccountDTO dto) {
		AssertUtil.assertNotNull(dto.getAddress(), "收货地址不能为空");
		BigDecimal totalPrice = BigDecimal.ZERO;
		BigDecimal realPrice = BigDecimal.ZERO;
		Order order = new Order();
		order.setClient(client);
		order.setMerchant(client.getMerchant());
		order.setOrderId(generateId());
		order.setStatus(OrderStatus.UnPay);
		order.setPayment(dto.getPayment());
		order.setRemark(dto.getRemark());
		order.setTotalPrice(totalPrice);
		order.setRealPrice(realPrice);
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
		BigDecimal discount = clientService.getCurrentLevel(client).getDiscount();
		realPrice = totalPrice.multiply(discount).multiply(new BigDecimal("0.01"));
		if (!CollectionUtils.isEmpty(dto.getCoupons())) {
			BigDecimal couponAmount = BigDecimal.ZERO;
			BigDecimal limitedAmount = getLimitedAmount(client.getMerchant(), realPrice);
			BigDecimal originalLimitedAmount = limitedAmount;
			for (ClientCouponDTO couponDTO : dto.getCoupons()) {
				switch (couponDTO.getType()) {
				case Cash: {
					Optional<ClientCoupon> couponOp = clientCouponRepository.findById(couponDTO.getId());
					if (couponOp.isPresent()) {
						limitedAmount = limitedAmount.subtract(couponDTO.getAmount());
						AssertUtil.assertTrue(limitedAmount.signum() >= 0,
								"超过优惠券限额，最多只能优惠" + originalLimitedAmount + "元");
						ClientCoupon coupon = couponOp.get();
						realPrice = realPrice.subtract(couponDTO.getAmount());
						couponAmount = couponAmount.add(couponDTO.getAmount());
						coupon.setUsedTime(new Date());
						coupon.setStatus(ClientCouponStatus.Used);
						coupon.setOrder(order);
						clientCouponRepository.save(coupon);
						em.lock(coupon.getSetting(), LockModeType.PESSIMISTIC_WRITE);
						coupon.getSetting().setUsedNums(coupon.getSetting().getUsedNums() + 1);
						couponSettingRepository.save(coupon.getSetting());
					}
				}
					break;
				}
				cacheEvictService.clearClientCoupons(order.getClient().getId());
			}
			order.setCouponAmount(couponAmount);
		}
		order.setTotalPrice(totalPrice);
		order.setRealPrice(realPrice);
		order.setSubList(subs);
		subOrderRepository.saveAll(subs);
		orderRepository.save(order);
		cacheEvictService.clearClientOrders(order.getClient().getId());
		return order.getId();
	}

	private BigDecimal getLimitedAmount(Merchant merchant, BigDecimal totalPrice) {
		BigDecimal limitedAmount = BigDecimal.ZERO;
		Optional<CashCouponOrderLimitedSetting> limitedOp = cashCouponOrderLimitedSettingRepository
				.findFirstByMerchantAndOrderAmountLessThanEqualOrderByLimitedAmountDesc(merchant, totalPrice);
		if (limitedOp.isPresent()) {
			limitedAmount = limitedOp.get().getLimitedAmount();
		}
		return limitedAmount;
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
		sub.setItem(item);
		sub.setItemName(item.getName());
		sub.setMarketPrice(item.getMarketPrice());
		sub.setNums(nums);
		sub.setParent(parent);
		sub.setUnitPrice(property.getPrice());
		sub.setSellingPoints(item.getSellingPoints());
		sub.setItemNumber(item.getNumber());
		StringBuilder spec = new StringBuilder();
		int index = 0;
		for (GoodsSpecificationValue value : property.getValues()) {
			spec.append(value.getName());
			if (index < property.getValues().size() - 1) {
				spec.append(", ");
			}
			index++;
		}
		sub.setSpecificationValues(spec.toString());
		em.lock(property, LockModeType.PESSIMISTIC_WRITE);
		int leftInventory = property.getInventory() - nums;
		AssertUtil.assertTrue(leftInventory >= 0, item.getName() + "库存不足");
		property.setInventory(leftInventory);
		propertyRepository.save(property);
		sub.setProperty(property);
		subOrderRepository.save(sub);
		try {
			byte[] buf = fileService.load(true, item.getThumbnail());
			String filePath = userDir + "/" + client.getId() + "/" + fileDir + "/" + sub.getId() + "/thumb";
			if (env.acceptsProfiles(Profiles.of(EnvConstant.LOCAL))) {
				filePath += "_" + System.currentTimeMillis();
			}
			filePath += ".png";
			fileService.upload(true, filePath, new ByteArrayInputStream(buf));
			sub.setThumbnail(filePath);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		subs.add(sub);
	}

	public void afterPayment(String orderId, String transactionId) {
		AssertUtil.assertNotEmpty(transactionId, "交易号有误");
		Optional<Order> orderOp = orderRepository.findByOrderId(orderId);
		if (orderOp.isPresent() && !orderOp.get().isDeleted() && OrderStatus.UnPay.equals(orderOp.get().getStatus())) {
			Order order = orderOp.get();
			em.lock(order, LockModeType.PESSIMISTIC_WRITE);
			order.setStatus(OrderStatus.Paid);
			order.setPayTime(new Date());
			order.setTransactionId(transactionId);
			orderRepository.save(order);
//			bonusService.addConsumeBonus(order);
			cacheEvictService.clearClientOrders(order.getClient().getId());
		}
	}

	@Transactional(readOnly = true)
	public PageResult<OrderDTO> getOrders(Client client, QueryInfo<OrderStatus> queryInfo) {
		Page<Order> orders = orderRepository.findAll(getSpec(client, queryInfo),
				queryInfo.getPage().pageable(Direction.DESC, "createTime"));
		return new PageResult<>(orders.get().map(order -> {
			OrderDTO dto = order.asDTO(true, false);
			if (!CollectionUtils.isEmpty(order.getSubList())) {
				int count = 0;
				int commentCount = 0;
				for (SubOrder sub : order.getSubList()) {
					if (sub.isComment()) {
						commentCount++;
					}
					Optional<AfterSaleApplication> app = aftersaleApplicationRepository.findBySubOrder(sub);
					if (app.isPresent() && !AfterSaleStatus.NotYet.equals(app.get().getStatus())) {
						count++;
					}
				}
				dto.setComment(commentCount == order.getSubList().size());
				dto.setApplied(count == order.getSubList().size());
			}
			return dto;
		}).collect(Collectors.toList()), orders.getTotalElements());
	}

	private Specification<Order> getSpec(Client client, QueryInfo<OrderStatus> queryInfo) {
		return new Specification<Order>() {

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
					if (!OrderStatus.Canceled.equals(queryInfo.getData())) {
						predicate.add(criteriaBuilder.equal(root.get("status"), queryInfo.getData()));
					} else {
						predicate
								.add(criteriaBuilder.or(criteriaBuilder.equal(root.get("status"), OrderStatus.Canceled),
										criteriaBuilder.equal(root.get("status"), OrderStatus.CanceledForRefund),
										criteriaBuilder.equal(root.get("status"), OrderStatus.RefundApplied),
										criteriaBuilder.equal(root.get("status"), OrderStatus.Closed),
										criteriaBuilder.equal(root.get("status"), OrderStatus.Refunded)));
					}
				}
				return query.where(predicate.toArray(new Predicate[] {})).getRestriction();
			}
		};
	}

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = CacheNameConstant.CLIENT_ORDER_NUMS, key = "#client.id.toString() + '-' + #queryInfo.data.name()")
	public long countOrders(Client client, QueryInfo<OrderStatus> queryInfo) {
		return orderRepository.count(getSpec(client, queryInfo));
	}

	@Transactional(readOnly = true)
	public OrderDTO getOrder(Client client, Long id) {
		Optional<Order> orderOp = orderRepository.findById(id);
		AssertUtil.assertTrue(
				orderOp.isPresent() && !orderOp.get().isDeleted()
						&& orderOp.get().getClient().getId().equals(client.getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		Order order = orderOp.get();
		OrderDTO dto = order.asDTO(true, true);
		if (!CollectionUtils.isEmpty(order.getSubList())) {
			int count = 0;
			int commentCount = 0;
			for (SubOrder sub : order.getSubList()) {
				if (sub.isComment()) {
					commentCount++;
				}
				Optional<AfterSaleApplication> app = aftersaleApplicationRepository.findBySubOrder(sub);
				if (app.isPresent() && !AfterSaleStatus.NotYet.equals(app.get().getStatus())) {
					count++;
				}
			}
			dto.setComment(commentCount == order.getSubList().size());
			dto.setApplied(count == order.getSubList().size());
		}
		return dto;
	}

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = CacheNameConstant.CLIENT_UNCOMMENT_NUMS, key = "#client.id")
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
		return subOrderRepository.count(spec);
	}

	public void cancel(Client client, Long id) throws Exception {
		Optional<Order> orderOp = orderRepository.findById(id);
		AssertUtil.assertTrue(
				orderOp.isPresent() && !orderOp.get().isDeleted()
						&& orderOp.get().getClient().getId().equals(client.getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		Order order = orderOp.get();
		em.lock(order, LockModeType.PESSIMISTIC_WRITE);
		OrderStatus status = order.getStatus();
		boolean paid = OrderStatus.Paid.equals(status);
		AssertUtil.assertTrue(OrderStatus.UnPay.equals(status) || paid, "不能取消订单");
//		if (paid) {
//			// 自动退款
//			switch (order.getPayment()) {
//			case WePay: {
//				WePayConfig config = wechatService.createWePayConfig(client.getMerchant().getId());
//				wechatService.refund(config, order.getTransactionId(), order.getOrderId(), order.getRealPrice(),
//						order.getRealPrice(), order.getSubList().size());
//			}
//				break;
//			case AliPay: {
//				alipayService.refund(client.getMerchant().getId(), order.getTransactionId(), order.getOrderId(),
//						order.getRealPrice(), order.getRealPrice(), order.getSubList().size());
//			}
//				break;
//			default:
//				throw new BusinessException("不支持的支付类型");
//			}
//		}
//		afterCancel(order.getOrderId());
		if (paid) {
			order.setStatus(OrderStatus.CanceledForRefund);
		} else {
			order.setStatus(OrderStatus.Canceled);
		}
		orderRepository.save(order);
		cacheEvictService.clearClientOrders(order.getClient().getId());
		List<SubOrder> list = order.getSubList();
		if (!CollectionUtils.isEmpty(list)) {
			for (SubOrder sub : list) {
				try {
					GoodsItemProperty property = sub.getProperty();
					if (property != null) {
						em.lock(property, LockModeType.PESSIMISTIC_WRITE);
						property.setInventory(property.getInventory() + sub.getNums());
						goodsItemPropertyRepository.save(property);
					}
				} catch (Exception e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}
//		bonusService.removeConsumeBonus(order);
	}

//	public void afterCancel(String orderId) {
//		Optional<Order> orderOp = orderRepository.findByOrderId(orderId);
//		if (orderOp.isPresent() && (OrderStatus.UnPay.equals(orderOp.get().getStatus())
//				|| OrderStatus.Paid.equals(orderOp.get().getStatus()))) {
//			Order order = orderOp.get();
//			em.lock(order, LockModeType.PESSIMISTIC_WRITE);
//			if (OrderStatus.Paid.equals(order.getStatus())) {
//				order.setRefundTime(new Date());
//			}
//			order.setStatus(OrderStatus.Canceled);
//			orderRepository.save(order);
//			cacheEvictService.clearClientOrders(order.getClient().getId());
//			List<SubOrder> list = order.getSubList();
//			if (!CollectionUtils.isEmpty(list)) {
//				for (SubOrder sub : list) {
//					GoodsItemProperty property = sub.getProperty();
//					if (property != null) {
//						em.lock(property, LockModeType.PESSIMISTIC_WRITE);
//						property.setInventory(property.getInventory() + sub.getNums());
//						goodsItemPropertyRepository.save(property);
//					}
//				}
//			}
//			List<ClientCoupon> coupons = order.getGeneratedCoupons();
//			if (!CollectionUtils.isEmpty(coupons)) {
//				for (ClientCoupon coupon : coupons) {
//					coupon.setStatus(ClientCouponStatus.Out);
//				}
//				clientCouponRepository.saveAll(coupons);
//				cacheEvictService.clearClientCoupons(order.getClient().getId());
//			}
//		}
//	}

	public void pickup(Client client, Long id) {
		Optional<Order> orderOp = orderRepository.findById(id);
		AssertUtil.assertTrue(
				orderOp.isPresent() && !orderOp.get().isDeleted()
						&& orderOp.get().getClient().getId().equals(client.getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		Order order = orderOp.get();
		AssertUtil.assertTrue(OrderStatus.Shipped.equals(order.getStatus()), "不能确认收货，订单状态有误");
		em.lock(order, LockModeType.PESSIMISTIC_WRITE);
		order.setStatus(OrderStatus.Finished);
		bonusService.addConsumeBonus(order);
		if (!CollectionUtils.isEmpty(order.getSubList())) {
			for (SubOrder sub : order.getSubList()) {
				if (sub.getItem() != null && sub.getItem().getId() != null) {
					GoodsItemRank rank = sub.getItem().getRank();
					em.lock(rank, LockModeType.PESSIMISTIC_WRITE);
					rank.setOrderNums(rank.getOrderNums() + 1);
					rankRepository.save(rank);
				}
				Optional<AfterSaleApplication> applicationOp = aftersaleApplicationRepository.findBySubOrder(sub);
				if (!applicationOp.isPresent()) {
					AfterSaleApplication application = new AfterSaleApplication();
					application.setClient(order.getClient());
					application.setMerchant(order.getMerchant());
					application.setOrder(order);
					application.setSubOrder(sub);
					application.setServiceId(generateId());
					application.setStatus(AfterSaleStatus.NotYet);
					aftersaleApplicationRepository.save(application);
				}
			}
		}
		cacheEvictService.clearClientOrders(order.getClient().getId());
//		cacheEvictService.clearAllGoodsItemList();
	}

	public void refundApply(Client client, Long id) {
		Optional<Order> orderOp = orderRepository.findById(id);
		AssertUtil.assertTrue(
				orderOp.isPresent() && !orderOp.get().isDeleted()
						&& orderOp.get().getClient().getId().equals(client.getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		Order order = orderOp.get();
		em.lock(order, LockModeType.PESSIMISTIC_WRITE);
		AssertUtil.assertTrue(OrderStatus.CanceledForRefund.equals(order.getStatus()), "状态有误，不能申请退款");
		order.setStatus(OrderStatus.RefundApplied);
		orderRepository.save(order);
		cacheEvictService.clearClientOrders(order.getClient().getId());
	}

	public void delete(Client client, Long id) {
		Optional<Order> order = orderRepository.findById(id);
		AssertUtil.assertTrue(
				order.isPresent() && !order.get().isDeleted() && order.get().getClient().getId().equals(client.getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		OrderStatus status = order.get().getStatus();
		AssertUtil.assertTrue(OrderStatus.Refunded.equals(status) || OrderStatus.Canceled.equals(status)
				|| OrderStatus.Closed.equals(status) || OrderStatus.Finished.equals(status), "不能清除订单，订单状态有误");
		order.get().setDeleted(true);
		orderRepository.save(order.get());
	}

	@Transactional(readOnly = true)
	public List<ClientCouponDTO> getAvailableCouponListForSettleAccount(Client client, OrderPreviewDTO dto) {
		BigDecimal totalPrice = BigDecimal.ZERO;
		for (OrderItemDTO item : dto.getItems()) {
			totalPrice = totalPrice.add(item.getProperty().getPrice().multiply(new BigDecimal(item.getNums())));
		}
		BigDecimal limitedAmount = getLimitedAmount(client.getMerchant(), totalPrice);
		List<ClientCoupon> selectedCoupons = new ArrayList<>();
		Specification<ClientCoupon> spec = new Specification<ClientCoupon>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Predicate toPredicate(Root<ClientCoupon> root, CriteriaQuery<?> query,
					CriteriaBuilder criteriaBuilder) {
				List<Predicate> predicate = new ArrayList<>();
				predicate.add(criteriaBuilder.equal(root.get("client"), client));
				predicate.add(criteriaBuilder.equal(root.get("status"), ClientCouponStatus.UnUse));
				return query.where(predicate.toArray(new Predicate[] {})).getRestriction();
			}
		};
		List<ClientCoupon> coupons = clientCouponRepository.findAll(spec);
		if (!CollectionUtils.isEmpty(coupons)) {
			for (ClientCoupon coupon : coupons) {
				if (coupon.getSetting().getAmount().compareTo(limitedAmount) <= 0) {
					for (OrderItemDTO item : dto.getItems()) {
						boolean hasPut = false;
						switch (coupon.getSetting().getHxType()) {
						case All:
							hasPut = putInSelectedCoupons(selectedCoupons, coupon);
							break;
						case Category: {
							if (coupon.getSetting().getRealCategories().stream()
									.anyMatch(it -> it.getId().equals(item.getItem().getCategoryId()))) {
								hasPut = putInSelectedCoupons(selectedCoupons, coupon);
							}
						}
							break;
						case Item: {
							if (coupon.getSetting().getItems().stream()
									.anyMatch(it -> it.getId().equals(item.getItem().getId()))) {
								hasPut = putInSelectedCoupons(selectedCoupons, coupon);
							}
						}
							break;
						}
						if (hasPut) {
							break;
						}
					}
				}
			}
		}
		return clientService.getCouponList(selectedCoupons.stream());
	}

	private boolean putInSelectedCoupons(List<ClientCoupon> selectedCoupons, ClientCoupon coupon) {
		if (coupon.getSetting().getLimitedNums() == 0
				|| selectedCoupons.stream().filter(it -> it.getSetting().getId().equals(coupon.getSetting().getId()))
						.count() < coupon.getSetting().getLimitedNums()) {
			selectedCoupons.add(coupon);
			return true;
		}
		return false;
	}
}
