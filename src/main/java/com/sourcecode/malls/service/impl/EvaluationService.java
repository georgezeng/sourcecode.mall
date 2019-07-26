package com.sourcecode.malls.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.goods.GoodsItemEvaluation;
import com.sourcecode.malls.domain.goods.GoodsItemEvaluationPhoto;
import com.sourcecode.malls.domain.goods.GoodsItemRank;
import com.sourcecode.malls.domain.order.SubOrder;
import com.sourcecode.malls.dto.goods.GoodsItemEvaluationDTO;
import com.sourcecode.malls.dto.order.SubOrderDTO;
import com.sourcecode.malls.dto.query.PageResult;
import com.sourcecode.malls.dto.query.QueryInfo;
import com.sourcecode.malls.enums.OrderStatus;
import com.sourcecode.malls.exception.BusinessException;
import com.sourcecode.malls.repository.jpa.impl.goods.GoodsItemEvaluationPhotoRepository;
import com.sourcecode.malls.repository.jpa.impl.goods.GoodsItemEvaluationRepository;
import com.sourcecode.malls.repository.jpa.impl.goods.GoodsItemRankRepository;
import com.sourcecode.malls.repository.jpa.impl.order.SubOrderRepository;
import com.sourcecode.malls.util.AssertUtil;

@Service
@Transactional
public class EvaluationService {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private GoodsItemEvaluationRepository repository;

	@Autowired
	private GoodsItemEvaluationPhotoRepository photoRepository;

	@Autowired
	private SubOrderRepository subOrderRepository;

	@Autowired
	private GoodsItemRankRepository rankRepository;

	@Autowired
	protected EntityManager em;

	@Transactional(readOnly = true)
	public PageResult<SubOrderDTO> getUnCommentList(Client client, QueryInfo<Long> queryInfo) {
		Specification<SubOrder> spec = new Specification<SubOrder>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Predicate toPredicate(Root<SubOrder> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
				List<Predicate> predicate = new ArrayList<>();
				predicate.add(criteriaBuilder.equal(root.get("client"), client));
				predicate.add(criteriaBuilder.equal(root.get("comment"), false));
				predicate.add(criteriaBuilder.equal(root.join("parent").get("status"), OrderStatus.Finished));
				if (queryInfo.getData() != null && queryInfo.getData() > 0) {
					predicate.add(criteriaBuilder.equal(root.get("parent"), queryInfo.getData()));
				}
				return query.where(predicate.toArray(new Predicate[] {})).getRestriction();
			}
		};
		Page<SubOrder> result = subOrderRepository.findAll(spec,
				queryInfo.getPage().pageable(Direction.ASC, "createTime"));
		return new PageResult<>(result.get().map(it -> it.asDTO()).collect(Collectors.toList()),
				result.getTotalElements());
	}

	@Transactional(readOnly = true)
	public PageResult<GoodsItemEvaluationDTO> getCommentList(Client client, QueryInfo<Long> queryInfo) {
		Specification<GoodsItemEvaluation> spec = new Specification<GoodsItemEvaluation>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Predicate toPredicate(Root<GoodsItemEvaluation> root, CriteriaQuery<?> query,
					CriteriaBuilder criteriaBuilder) {
				List<Predicate> predicate = new ArrayList<>();
				predicate.add(criteriaBuilder.equal(root.get("client"), client));
				predicate.add(criteriaBuilder.equal(root.get("additional"), false));
				if (queryInfo.getData() != null && queryInfo.getData() > 0) {
					predicate.add(criteriaBuilder.equal(root.get("order"), queryInfo.getData()));
				}
				return query.where(predicate.toArray(new Predicate[] {})).getRestriction();
			}
		};
		Page<GoodsItemEvaluation> result = repository.findAll(spec,
				queryInfo.getPage().pageable(Direction.DESC, "createTime"));
		return new PageResult<>(result.get().map(it -> {
			GoodsItemEvaluationDTO dto = it.asDTO(true);
			dto.setItemName(it.getSubOrder().getItemName());
			dto.setItemThumbnail(it.getSubOrder().getThumbnail());
			dto.setItemNums(it.getSubOrder().getNums());
			dto.setItemSpecificationValues(it.getSubOrder().getSpecificationValues());
			return dto;
		}).collect(Collectors.toList()), result.getTotalElements());
	}

	@Transactional(readOnly = true)
	public PageResult<GoodsItemEvaluationDTO> getCommentListForGoodsItem(Client client,
			QueryInfo<GoodsItemEvaluationDTO> queryInfo) {
		Specification<GoodsItemEvaluation> spec = new Specification<GoodsItemEvaluation>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Predicate toPredicate(Root<GoodsItemEvaluation> root, CriteriaQuery<?> query,
					CriteriaBuilder criteriaBuilder) {
				List<Predicate> predicate = new ArrayList<>();
				predicate.add(criteriaBuilder.equal(root.get("client"), client));
				predicate.add(criteriaBuilder.equal(root.get("passed"), true));
				predicate.add(criteriaBuilder.equal(root.get("additional"), false));
				predicate.add(criteriaBuilder.equal(root.get("open"), true));
				if (queryInfo.getData() != null) {
					if (queryInfo.getData().getId() != null && queryInfo.getData().getId() > 0) {
						predicate.add(criteriaBuilder.equal(root.get("item"), queryInfo.getData().getId()));
					}
					if (queryInfo.getData().getValue() != null) {
						predicate.add(criteriaBuilder.equal(root.get("value"), queryInfo.getData().getValue()));
					}
				}
				return query.where(predicate.toArray(new Predicate[] {})).getRestriction();
			}
		};
		Page<GoodsItemEvaluation> result = repository.findAll(spec,
				queryInfo.getPage().pageable(Direction.DESC, "createTime"));
		return new PageResult<>(result.get().map(it -> {
			GoodsItemEvaluationDTO dto = it.asDTO(false);
			dto.setItemName(it.getSubOrder().getItemName());
			dto.setItemThumbnail(it.getSubOrder().getThumbnail());
			dto.setItemNums(it.getSubOrder().getNums());
			dto.setItemSpecificationValues(it.getSubOrder().getSpecificationValues());
			return dto;
		}).collect(Collectors.toList()), result.getTotalElements());
	}

	public void save(Client client, GoodsItemEvaluationDTO dto) {
		AssertUtil.assertNotNull(dto.getId(), "找不到订单序号");
		Optional<SubOrder> subOrder = subOrderRepository.findById(dto.getId());
		AssertUtil.assertTrue(subOrder.isPresent() && subOrder.get().getClient().getId().equals(client.getId()),
				"找不到订单数据");
		Optional<GoodsItemEvaluation> dataOp = repository.findBySubOrder(subOrder.get());
		AssertUtil.assertTrue(!dataOp.isPresent(), "此订单商品已经做过评价");
		GoodsItemEvaluation data = dto.asEntity();
		data.setClient(client);
		data.setMerchant(client.getMerchant());
		data.setSubOrder(subOrder.get());
		data.setOrder(subOrder.get().getParent());
		data.setItem(subOrder.get().getItem());
		data.setPassed(false);
		data.setReplied(false);
		data.setReply(null);
		data.setAdditional(false);
		data.setOpen(false);
		repository.save(data);
		if (!CollectionUtils.isEmpty(dto.getPhotos())) {
			AssertUtil.assertTrue(dto.getPhotos().size() <= 5, "最多上传5张图片");
			for (String path : dto.getPhotos()) {
				GoodsItemEvaluationPhoto photo = new GoodsItemEvaluationPhoto();
				photo.setEvaluation(data);
				photo.setPath(path);
				photoRepository.save(photo);
			}
		}
		subOrder.get().setComment(true);
		subOrderRepository.save(subOrder.get());
		GoodsItemRank rank = subOrder.get().getItem().getRank();
		em.lock(rank, LockModeType.PESSIMISTIC_WRITE);
		switch (data.getValue()) {
		case Bad:
			rank.setBadEvaluations(rank.getBadEvaluations() + 1);
			break;
		case Neutrality:
			rank.setNeutralityEvaluations(rank.getNeutralityEvaluations() + 1);
			break;
		case Good:
			rank.setGoodEvaluations(rank.getGoodEvaluations() + 1);
			break;
		default:
			throw new BusinessException("不支持的评价级别");
		}
		rankRepository.save(rank);
	}

	public void saveAdditional(Client client, GoodsItemEvaluationDTO dto) {
		AssertUtil.assertNotNull(dto.getId(), "找不到评价序号");
		Optional<GoodsItemEvaluation> evaluationOp = repository.findById(dto.getId());
		AssertUtil.assertTrue(evaluationOp.isPresent() && evaluationOp.get().getClient().getId().equals(client.getId())
				&& !evaluationOp.get().isAdditional(), "找不到评价数据");
		GoodsItemEvaluation evaluation = evaluationOp.get();
		AssertUtil.assertTrue(
				evaluation.getAdditionalEvaluation() == null || evaluation.getAdditionalEvaluation().getId() == null,
				"此订单商品已经追加了评价");
		AssertUtil.assertTrue(evaluation.isPassed(), "不能追加评价");
		GoodsItemEvaluation data = dto.asEntity();
		data.setClient(client);
		data.setMerchant(client.getMerchant());
		data.setSubOrder(evaluation.getSubOrder());
		data.setOrder(evaluation.getSubOrder().getParent());
		data.setItem(evaluation.getSubOrder().getItem());
		data.setPassed(false);
		data.setReplied(false);
		data.setReply(null);
		data.setAdditional(true);
		data.setOpen(false);
		repository.save(data);
		evaluation.setAdditionalEvaluation(data);
		repository.save(evaluation);
		if (!CollectionUtils.isEmpty(dto.getPhotos())) {
			AssertUtil.assertTrue(dto.getPhotos().size() <= 5, "最多上传5张图片");
			for (String path : dto.getPhotos()) {
				GoodsItemEvaluationPhoto photo = new GoodsItemEvaluationPhoto();
				photo.setEvaluation(data);
				photo.setPath(path);
				photoRepository.save(photo);
			}
		}
	}
}
