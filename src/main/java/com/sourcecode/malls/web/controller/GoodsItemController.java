package com.sourcecode.malls.web.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.goods.GoodsItem;
import com.sourcecode.malls.domain.goods.GoodsItemEvaluation;
import com.sourcecode.malls.domain.goods.GoodsSpecificationDefinition;
import com.sourcecode.malls.dto.base.KeyDTO;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.goods.GoodsAttributeDTO;
import com.sourcecode.malls.dto.goods.GoodsItemDTO;
import com.sourcecode.malls.dto.query.QueryInfo;
import com.sourcecode.malls.repository.jpa.impl.goods.GoodsItemEvaluationRepository;
import com.sourcecode.malls.repository.jpa.impl.goods.GoodsSpecificationDefinitionRepository;
import com.sourcecode.malls.service.impl.GoodsItemService;

@RestController
@RequestMapping(path = "/goods/item")
public class GoodsItemController {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private GoodsItemService service;

	@Autowired
	private GoodsItemEvaluationRepository evaluationRepository;

	@Autowired
	private GoodsSpecificationDefinitionRepository definitionRepository;

	@RequestMapping(path = "/list/params/{id}/{type}")
	public ResultBean<GoodsItemDTO> list(@PathVariable("id") Long categoryId, @PathVariable("type") String type,
			@RequestBody QueryInfo<String> queryInfo) {
		Page<GoodsItem> result = service.findByCategory(ClientContext.getMerchantId(), categoryId, type, queryInfo);
		return new ResultBean<>(
				result.getContent().stream().map(it -> it.asDTO(false, false, false)).collect(Collectors.toList()));
	}

	@RequestMapping(path = "/definitions/load")
	public ResultBean<GoodsAttributeDTO> loadDefinitions(@RequestBody KeyDTO<Long> dto) {
		List<GoodsAttributeDTO> list = new ArrayList<>();
		for (Long id : dto.getIds()) {
			Optional<GoodsSpecificationDefinition> definition = definitionRepository.findById(id);
			if (definition.isPresent()
					&& definition.get().getMerchant().getId().equals(ClientContext.getMerchantId())) {
				list.add(definition.get().asDTO());
			}
		}
		return new ResultBean<>(list);
	}

	@RequestMapping(path = "/load/params/{id}")
	public ResultBean<GoodsItemDTO> load(@PathVariable("id") Long id) {
		GoodsItemDTO dto = service.load(ClientContext.getMerchantId(), id);
		Optional<GoodsItem> item = service.findById(dto.getId());
		Optional<GoodsItemEvaluation> eva = evaluationRepository
				.findFirstByItemAndPassedOrderByCreateTimeDesc(item.get(), true);
		if (eva.isPresent()) {
			dto.setTopEvaluation(eva.get().asDTO(false));
		}
		return new ResultBean<>(dto);
	}

}
