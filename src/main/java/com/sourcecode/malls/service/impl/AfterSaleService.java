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
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.sourcecode.malls.constants.ExceptionMessageConstant;
import com.sourcecode.malls.domain.aftersale.AfterSaleApplication;
import com.sourcecode.malls.domain.aftersale.AfterSalePhoto;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.order.Order;
import com.sourcecode.malls.domain.order.SubOrder;
import com.sourcecode.malls.dto.aftersale.AfterSaleApplicationDTO;
import com.sourcecode.malls.dto.query.PageInfo;
import com.sourcecode.malls.enums.AfterSaleStatus;
import com.sourcecode.malls.enums.AfterSaleType;
import com.sourcecode.malls.enums.OrderStatus;
import com.sourcecode.malls.exception.BusinessException;
import com.sourcecode.malls.repository.jpa.impl.aftersale.AfterSaleApplicationRepository;
import com.sourcecode.malls.repository.jpa.impl.aftersale.AfterSalePhotoRepository;
import com.sourcecode.malls.repository.jpa.impl.order.OrderRepository;
import com.sourcecode.malls.service.base.BaseService;
import com.sourcecode.malls.util.AssertUtil;

@Service
@Transactional
public class AfterSaleService implements BaseService {

	@Autowired
	private AfterSalePhotoRepository photoRepository;

	@Autowired
	private AfterSaleApplicationRepository applicationRepository;

	@Autowired
	private OrderRepository orderRepository;

	public void applyRefund(Long clientId, AfterSaleApplicationDTO dto) {
		AssertUtil.assertNotEmpty(dto.getDescription(), "描述不能为空");
		AssertUtil.assertNotEmpty(dto.getReason(), "原因不能为空");
		if (!CollectionUtils.isEmpty(dto.getPhotos())) {
			AssertUtil.assertTrue(dto.getPhotos().size() <= 5, "最多上传5张图片");
		}
		AssertUtil.assertNotNull(dto.getId(), "订单序号不能为空");
		Optional<Order> orderOp = orderRepository.findById(dto.getId());
		AssertUtil.assertTrue(orderOp.isPresent() && orderOp.get().getClient().getId().equals(clientId),
				ExceptionMessageConstant.NO_SUCH_RECORD);
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
	}

	public void apply(Long clientId, AfterSaleApplicationDTO dto) {
		AssertUtil.assertNotEmpty(dto.getDescription(), "描述不能为空");
		AssertUtil.assertNotEmpty(dto.getReason(), "原因不能为空");
		if (!CollectionUtils.isEmpty(dto.getPhotos())) {
			AssertUtil.assertTrue(dto.getPhotos().size() <= 5, "最多上传5张图片");
		}
		Optional<AfterSaleApplication> dataOp = applicationRepository.findById(dto.getId());
		AssertUtil.assertTrue(dataOp.isPresent() && dataOp.get().getClient().getId().equals(clientId),
				ExceptionMessageConstant.NO_SUCH_RECORD);

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
			data.setAddress(dto.getAddress().asAfterSaleAddressEntity());
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
		if (!CollectionUtils.isEmpty(dto.getPhotos())) {
			for (String path : dto.getPhotos()) {
				AfterSalePhoto photo = new AfterSalePhoto();
				photo.setApplication(data);
				photo.setPath(path);
				photoRepository.save(photo);
			}
		}
	}

	@Transactional(readOnly = true)
	public List<AfterSaleApplicationDTO> list(Client client, Long orderId, String status, PageInfo pageInfo) {
		Order order = null;
		if (orderId != null && orderId > 0) {
			Optional<Order> orderOp = orderRepository.findById(orderId);
			AssertUtil.assertTrue(orderOp.isPresent() && orderOp.get().getClient().getId().equals(client.getId()),
					ExceptionMessageConstant.NO_SUCH_RECORD);
			order = orderOp.get();
		}
		final Order theOrder = order;
		Specification<AfterSaleApplication> spec = new Specification<AfterSaleApplication>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Predicate toPredicate(Root<AfterSaleApplication> root, CriteriaQuery<?> query,
					CriteriaBuilder criteriaBuilder) {
				List<Predicate> predicate = new ArrayList<>();
				if (theOrder != null) {
					predicate.add(criteriaBuilder.equal(root.get("order"), theOrder));
				} else {
					predicate.add(criteriaBuilder.equal(root.get("client"), client));
				}
				if ("WaitForProcess".equals(status)) {
					predicate.add(
							criteriaBuilder.or(criteriaBuilder.equal(root.get("status"), AfterSaleStatus.WaitForReturn),
									criteriaBuilder.equal(root.get("status"), AfterSaleStatus.WaitForPickup)));
				} else if ("WaitForConfirm".equals(status)) {
					predicate.add(
							criteriaBuilder.or(criteriaBuilder.equal(root.get("status"), AfterSaleStatus.WaitForRefund),
									criteriaBuilder.equal(root.get("status"), AfterSaleStatus.WaitForReceive)));
				} else if ("Finished".equals("status")) {
					predicate
							.add(criteriaBuilder.or(criteriaBuilder.equal(root.get("status"), AfterSaleStatus.Finished),
									criteriaBuilder.equal(root.get("status"), AfterSaleStatus.Rejected)));
				} else {
					predicate.add(criteriaBuilder.equal(root.get("status"), AfterSaleStatus.valueOf(status)));
				}
				return query.where(predicate.toArray(new Predicate[] {})).getRestriction();
			}
		};

		Page<AfterSaleApplication> result = applicationRepository.findAll(spec, pageInfo.pageable());
		return result.get().map(it -> it.asDTO()).collect(Collectors.toList());
	}

}
