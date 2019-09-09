package com.sourcecode.malls.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.sourcecode.malls.constants.CacheNameConstant;
import com.sourcecode.malls.constants.ExceptionMessageConstant;
import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.aftersale.AfterSaleAddress;
import com.sourcecode.malls.domain.aftersale.AfterSaleApplication;
import com.sourcecode.malls.domain.aftersale.AfterSalePhoto;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.merchant.Merchant;
import com.sourcecode.malls.domain.order.Order;
import com.sourcecode.malls.domain.order.SubOrder;
import com.sourcecode.malls.domain.redis.SearchCacheKeyStore;
import com.sourcecode.malls.dto.aftersale.AfterSaleApplicationDTO;
import com.sourcecode.malls.dto.order.ExpressDTO;
import com.sourcecode.malls.dto.query.PageInfo;
import com.sourcecode.malls.enums.AfterSaleStatus;
import com.sourcecode.malls.enums.AfterSaleType;
import com.sourcecode.malls.enums.OrderStatus;
import com.sourcecode.malls.exception.BusinessException;
import com.sourcecode.malls.repository.jpa.impl.aftersale.AfterSaleAddressRepository;
import com.sourcecode.malls.repository.jpa.impl.aftersale.AfterSaleApplicationRepository;
import com.sourcecode.malls.repository.jpa.impl.aftersale.AfterSalePhotoRepository;
import com.sourcecode.malls.repository.jpa.impl.aftersale.AfterSaleReasonSettingRepository;
import com.sourcecode.malls.repository.jpa.impl.order.OrderRepository;
import com.sourcecode.malls.repository.redis.impl.SearchCacheKeyStoreRepository;
import com.sourcecode.malls.service.base.BaseService;
import com.sourcecode.malls.util.AssertUtil;

@Service
@Transactional
public class AfterSaleService implements BaseService {
	@Autowired
	private AfterSaleReasonSettingRepository settingRepository;

	@Autowired
	private AfterSalePhotoRepository photoRepository;

	@Autowired
	private AfterSaleApplicationRepository applicationRepository;

	@Autowired
	private AfterSaleAddressRepository addressRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private CacheEvictService cacheEvictService;

	@Autowired
	private CacheClearer clearer;

	@Autowired
	private SearchCacheKeyStoreRepository searchCacheKeyStoreRepository;

	@Cacheable(cacheNames = CacheNameConstant.AFTER_SALE_REASON_LIST, key = "#merchant.id + '-' + #type.name()")
	public List<String> getAllReasons(Merchant merchant, AfterSaleType type) {
		return settingRepository.findAllByMerchantAndType(merchant, type).stream().map(it -> it.getContent()).collect(Collectors.toList());
	}

	@Cacheable(cacheNames = CacheNameConstant.AFTER_SALE_LOAD_ONE, key = "#id")
	public AfterSaleApplicationDTO load(Long clientId, Long id) {
		Optional<AfterSaleApplication> data = applicationRepository.findById(id);
		AssertUtil.assertTrue(data.isPresent() && data.get().getClient().getId().equals(clientId), ExceptionMessageConstant.NO_SUCH_RECORD);
		return data.get().asDTO();
	}

	public void applyRefund(Long clientId, AfterSaleApplicationDTO dto) {
		AssertUtil.assertNotEmpty(dto.getDescription(), "描述不能为空");
		AssertUtil.assertNotEmpty(dto.getReason(), "原因不能为空");
		if (!CollectionUtils.isEmpty(dto.getPhotos())) {
			AssertUtil.assertTrue(dto.getPhotos().size() <= 5, "最多上传5张图片");
		}
		AssertUtil.assertNotNull(dto.getId(), "订单序号不能为空");
		Optional<Order> orderOp = orderRepository.findById(dto.getId());
		AssertUtil.assertTrue(orderOp.isPresent() && orderOp.get().getClient().getId().equals(clientId), ExceptionMessageConstant.NO_SUCH_RECORD);
		Order order = orderOp.get();
		AssertUtil.assertTrue(OrderStatus.Shipped.equals(order.getStatus()), "订单状态有误，不能申请退款");
		for (SubOrder sub : order.getSubList()) {
			Optional<AfterSaleApplication> appOp = applicationRepository.findBySubOrder(sub);
			AssertUtil.assertTrue(!appOp.isPresent(), "申请记录已存在");
			AfterSaleApplication application = new AfterSaleApplication();
			application.setClient(order.getClient());
			application.setMerchant(order.getMerchant());
			application.setOrder(order);
			application.setSubOrder(sub);
			application.setServiceId(generateId());
			application.setType(AfterSaleType.RefundOnly);
			application.setStatus(AfterSaleStatus.Processing);
			application.setNums(sub.getNums());
			application.setAmount(sub.getDealPrice());
			application.setPostTime(new Date());
			application.setDescription(dto.getDescription());
			application.setReason(dto.getReason());
			applicationRepository.save(application);
			clearer.clearAfterSales(application);
			cacheEvictService.clearClientAfterSaleUnFinishedtNums(clientId);
			if (!CollectionUtils.isEmpty(dto.getPhotos())) {
				for (String path : dto.getPhotos()) {
					AfterSalePhoto photo = new AfterSalePhoto();
					photo.setApplication(application);
					photo.setPath(path);
					photoRepository.save(photo);
				}
			}
		}
		order.setStatus(OrderStatus.Closed);
		orderRepository.save(order);
		clearer.clearClientOrders(order);
	}

	public void apply(Long clientId, AfterSaleApplicationDTO dto) {
		AssertUtil.assertNotEmpty(dto.getDescription(), "描述不能为空");
		AssertUtil.assertNotEmpty(dto.getReason(), "原因不能为空");
		if (!CollectionUtils.isEmpty(dto.getPhotos())) {
			AssertUtil.assertTrue(dto.getPhotos().size() <= 5, "最多上传5张图片");
		}
		Optional<AfterSaleApplication> dataOp = applicationRepository.findById(dto.getId());
		AssertUtil.assertTrue(dataOp.isPresent() && dataOp.get().getClient().getId().equals(clientId), ExceptionMessageConstant.NO_SUCH_RECORD);

		AfterSaleApplication data = dataOp.get();
		AssertUtil.assertTrue(OrderStatus.Finished.equals(data.getOrder().getStatus()), "订单状态有误，不能申请售后");
		AssertUtil.assertTrue(AfterSaleStatus.NotYet.equals(data.getStatus()), "已经申请过售后");
		switch (dto.getType()) {
		case Change: {
			AssertUtil.assertNotNull(dto.getAddress(), "地址不能为空");
			AssertUtil.assertTrue(dto.getNums() > 0 && dto.getNums() <= data.getSubOrder().getNums(), "数量不正确");
//			AssertUtil.assertNotEmpty(dto.getSpecificationValues(), "规格不能为空");
//			AssertUtil.assertNotNull(dto.getPropertyId(), "规格选择有误");
//			Optional<GoodsItemProperty> propertyOp = propertyRepository.findById(dto.getPropertyId());
//			AssertUtil.assertTrue(
//					propertyOp.isPresent() && propertyOp.get().getItem().getId().equals(data.getSubOrder().getItemId()),
//					"找不到此规格");
//			GoodsItemProperty property = propertyOp.get();
//			em.lock(property, LockModeType.PESSIMISTIC_WRITE);
//			int leftInventory = property.getInventory() - dto.getNums();
//			AssertUtil.assertTrue(leftInventory >= 0, "库存不足");
//			property.setInventory(leftInventory);
//			propertyRepository.save(property);
//			data.setSpecificationValues(dto.getSpecificationValues());
			data.setNums(dto.getNums());
		}
			break;
		case SalesReturn: {
			AssertUtil.assertTrue(dto.getNums() > 0 && dto.getNums() <= data.getSubOrder().getNums(), "数量不正确");
			data.setNums(dto.getNums());
			data.setAmount(data.getSubOrder().getUnitPrice().multiply(new BigDecimal(dto.getNums())));
		}
			break;
		default:
			throw new BusinessException("目前只能进行退货退款或者换货");
		}
		data.setPostTime(new Date());
		data.setDescription(dto.getDescription());
		data.setReason(dto.getReason());
		data.setType(dto.getType());
		data.setStatus(AfterSaleStatus.Processing);
		applicationRepository.save(data);
		if (AfterSaleType.Change.equals(dto.getType())) {
			AfterSaleAddress address = dto.getAddress().asAfterSaleAddressEntity();
			address.setApplication(data);
			addressRepository.save(address);
		}
		if (!CollectionUtils.isEmpty(dto.getPhotos())) {
			for (String path : dto.getPhotos()) {
				AfterSalePhoto photo = new AfterSalePhoto();
				photo.setApplication(data);
				photo.setPath(path);
				photoRepository.save(photo);
			}
		}
		cacheEvictService.clearClientAfterSaleUnFinishedtNums(clientId);
		clearer.clearAfterSales(data);
	}

	public void fillExpress(ExpressDTO dto) {
		AssertUtil.assertNotEmpty(dto.getCompany(), "物流公司不能为空");
		AssertUtil.assertNotEmpty(dto.getNumber(), "物流单号不能为空");
		Optional<AfterSaleApplication> dataOp = applicationRepository.findById(dto.getId());
		AssertUtil.assertTrue(dataOp.isPresent() && dataOp.get().getClient().getId().equals(ClientContext.get().getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		AfterSaleApplication data = dataOp.get();
		AssertUtil.assertTrue(AfterSaleType.SalesReturn.equals(data.getType()) || AfterSaleType.Change.equals(data.getType()), "售后类型不是退换货服务，不能提交运单信息");
		AssertUtil.assertTrue(AfterSaleStatus.WaitForReturn.equals(data.getStatus()), "售后状态有误，不允许提交运单信息");
		data.setClientExpressCompany(dto.getCompany());
		data.setClientExpressNumber(dto.getNumber());
		data.setStatus(AfterSaleStatus.WaitForReceive);
		data.setReturnTime(new Date());
		applicationRepository.save(data);
		clearer.clearAfterSales(data);
	}

	public void pickup(Long id) {
		Optional<AfterSaleApplication> dataOp = applicationRepository.findById(id);
		AssertUtil.assertTrue(dataOp.isPresent() && dataOp.get().getClient().getId().equals(ClientContext.get().getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		AfterSaleApplication data = dataOp.get();
		AssertUtil.assertTrue(AfterSaleStatus.WaitForPickup.equals(data.getStatus()), "售后记录状态有误，不能确认收货");
		data.setStatus(AfterSaleStatus.Finished);
		data.setPickupTime(new Date());
		applicationRepository.save(data);
		cacheEvictService.clearClientAfterSaleUnFinishedtNums(data.getClient().getId());
		clearer.clearAfterSales(data);
	}

	@Cacheable(cacheNames = CacheNameConstant.AFTER_SALE_LIST, key = "#client.id + '-' + #orderId + '-' + #status + '-' + #pageInfo.num")
	@Transactional(readOnly = true)
	public List<AfterSaleApplicationDTO> list(Client client, Long orderId, String status, PageInfo pageInfo) {
		String key = client.getId() + "-" + orderId + "-" + status + "-" + pageInfo.getNum();
		SearchCacheKeyStore store = new SearchCacheKeyStore();
		store.setType(SearchCacheKeyStore.SEARCH_AFTER_SALE);
		store.setBizKey(client.getId() + "-" + orderId);
		store.setSearchKey(key);
		searchCacheKeyStoreRepository.save(store);
		Order order = null;
		if (orderId != null && orderId > 0) {
			Optional<Order> orderOp = orderRepository.findById(orderId);
			AssertUtil.assertTrue(orderOp.isPresent() && orderOp.get().getClient().getId().equals(client.getId()), ExceptionMessageConstant.NO_SUCH_RECORD);
			order = orderOp.get();
		}
		final Order theOrder = order;
		Specification<AfterSaleApplication> spec = new Specification<AfterSaleApplication>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Predicate toPredicate(Root<AfterSaleApplication> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
				List<Predicate> predicate = new ArrayList<>();
				if (theOrder != null) {
					predicate.add(criteriaBuilder.equal(root.get("order"), theOrder));
				} else {
					predicate.add(criteriaBuilder.equal(root.get("client"), client));
				}
//				if ("WaitForProcess".equals(status)) {
//					predicate.add(
//							criteriaBuilder.or(criteriaBuilder.equal(root.get("status"), AfterSaleStatus.WaitForReturn),
//									criteriaBuilder.equal(root.get("status"), AfterSaleStatus.WaitForPickup)));
//				} else if ("WaitForConfirm".equals(status)) {
//					predicate.add(
//							criteriaBuilder.or(criteriaBuilder.equal(root.get("status"), AfterSaleStatus.WaitForRefund),
//									criteriaBuilder.equal(root.get("status"), AfterSaleStatus.WaitForReceive)));
//				} else if ("Finished".equals("status")) {
//					predicate
//							.add(criteriaBuilder.or(criteriaBuilder.equal(root.get("status"), AfterSaleStatus.Finished),
//									criteriaBuilder.equal(root.get("status"), AfterSaleStatus.Rejected)));
//				} else {
//					predicate.add(criteriaBuilder.equal(root.get("status"), AfterSaleStatus.valueOf(status)));
//				}
				if (!"all".equalsIgnoreCase(status)) {
					predicate.add(criteriaBuilder.equal(root.get("status"), AfterSaleStatus.valueOf(status)));
				} else {
					predicate.add(criteriaBuilder.notEqual(root.get("status"), AfterSaleStatus.NotYet));
				}
				return query.where(predicate.toArray(new Predicate[] {})).getRestriction();
			}
		};

		Page<AfterSaleApplication> result = applicationRepository.findAll(spec, pageInfo.pageable());
		return result.get().map(it -> it.asDTO()).collect(Collectors.toList());
	}

	@Cacheable(cacheNames = CacheNameConstant.CLIENT_AFTERSALE_UNFINISHED_NUMS, key = "#client.id")
	public long countUnFinished(Client client) {
		Specification<AfterSaleApplication> spec = new Specification<AfterSaleApplication>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Predicate toPredicate(Root<AfterSaleApplication> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
				List<Predicate> predicate = new ArrayList<>();
				predicate.add(criteriaBuilder.equal(root.get("client"), client));
				predicate.add(criteriaBuilder.notEqual(root.get("status"), AfterSaleStatus.NotYet));
				predicate.add(criteriaBuilder.notEqual(root.get("status"), AfterSaleStatus.Finished));
				return query.where(predicate.toArray(new Predicate[] {})).getRestriction();
			}
		};
		return applicationRepository.count(spec);
	}

}
