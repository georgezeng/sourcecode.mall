package com.sourcecode.malls.service.impl;

import java.util.ArrayList;
import java.util.List;
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

import com.sourcecode.malls.constants.CacheNameConstant;
import com.sourcecode.malls.domain.merchant.AdvertisementSetting;
import com.sourcecode.malls.dto.merchant.AdvertisementSettingDTO;
import com.sourcecode.malls.dto.query.QueryInfo;
import com.sourcecode.malls.enums.AdvertisementType;
import com.sourcecode.malls.repository.jpa.impl.merchant.AdvertisementSettingRepository;

@Service
@Transactional
public class AdvertisementService {

	@Autowired
	private AdvertisementSettingRepository repository;

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = CacheNameConstant.ADVERTISEMENT_LIST, key = "#merchantId + '-' + #queryInfo.data.name()")
	public List<AdvertisementSettingDTO> getList(Long merchantId, QueryInfo<AdvertisementType> queryInfo) {
		Page<AdvertisementSetting> page = null;
//		queryInfo.getPage().setOrder(Direction.DESC.name());
//		queryInfo.getPage().setProperty("orderNum");
		Specification<AdvertisementSetting> spec = new Specification<AdvertisementSetting>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Predicate toPredicate(Root<AdvertisementSetting> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
//				Date now = new Date();
				List<Predicate> predicate = new ArrayList<>();
				predicate.add(criteriaBuilder.equal(root.get("merchant"), merchantId));
				predicate.add(criteriaBuilder.equal(root.get("enabled"), true));
//				predicate.add(criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), now));
//				predicate.add(criteriaBuilder.greaterThanOrEqualTo(root.get("endDate"), now));
				if (queryInfo.getData() != null) {
					predicate.add(criteriaBuilder.equal(root.get("type"), queryInfo.getData()));
				}
				return query.where(predicate.toArray(new Predicate[] {})).getRestriction();
			}
		};
		page = repository.findAll(spec, queryInfo.getPage().pageable());
		return page.get().map(it -> it.asDTO()).collect(Collectors.toList());
	}

}
