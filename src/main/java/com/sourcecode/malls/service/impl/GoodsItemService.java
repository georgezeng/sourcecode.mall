package com.sourcecode.malls.service.impl;

import java.util.ArrayList;
import java.util.List;

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

import com.sourcecode.malls.domain.goods.GoodsItem;
import com.sourcecode.malls.dto.query.PageInfo;
import com.sourcecode.malls.repository.jpa.impl.goods.GoodsItemRepository;

@Service
@Transactional
public class GoodsItemService {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private GoodsItemRepository itemRepository;

	@Transactional(readOnly = true)
	public Page<GoodsItem> findByCategory(Long merchantId, Long categoryId, String type, PageInfo pageInfo) {
		Specification<GoodsItem> spec = new Specification<GoodsItem>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Predicate toPredicate(Root<GoodsItem> root, CriteriaQuery<?> query,
					CriteriaBuilder criteriaBuilder) {
				List<Predicate> predicate = new ArrayList<>();
				predicate.add(criteriaBuilder.equal(root.get("merchant"), merchantId));
				predicate.add(criteriaBuilder.equal(root.get("category"), categoryId));
				predicate.add(criteriaBuilder.equal(root.get("enabled"), true));
				return query.where(predicate.toArray(new Predicate[] {})).getRestriction();
			}
		};
		switch (type) {
		case "putTime":
			return itemRepository.findAll(spec, pageInfo.pageable(pageInfo.getOrder(), "putTime"));
		case "realPrice":
			return itemRepository.findAll(spec, pageInfo.pageable(pageInfo.getOrder(), "realPrice", "putTime"));
		default:
			return itemRepository.findAll(spec,
					pageInfo.pageable(Direction.DESC, "rank.orderNums", "rank.goodPoints", "putTime"));
		}

	}
}
