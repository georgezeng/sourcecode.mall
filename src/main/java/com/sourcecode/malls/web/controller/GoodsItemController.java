package com.sourcecode.malls.web.controller;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.dto.base.KeyDTO;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.goods.GoodsAttributeDTO;
import com.sourcecode.malls.dto.goods.GoodsItemDTO;
import com.sourcecode.malls.dto.goods.GoodsItemEvaluationDTO;
import com.sourcecode.malls.dto.query.QueryInfo;
import com.sourcecode.malls.service.impl.EvaluationService;
import com.sourcecode.malls.service.impl.GoodsItemService;

@RestController
@RequestMapping(path = "/goods/item")
public class GoodsItemController {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private GoodsItemService service;

	@Autowired
	private EvaluationService evaService;

	@RequestMapping(path = "/list/params/{queryType}/{id}/{sortType}")
	public ResultBean<GoodsItemDTO> list(@PathVariable("queryType") String queryType, @PathVariable("id") Long id,
			@PathVariable("sortType") String sortType, @RequestBody QueryInfo<String> queryInfo) {
		switch (queryType) {
		case "category": {
			return new ResultBean<>(service.findByCategory(ClientContext.getMerchantId(), id, sortType, queryInfo));
		}
		case "coupon": {
			return new ResultBean<>(service.findByCoupon(ClientContext.getMerchantId(), id, sortType, queryInfo));
		}
		}
		return new ResultBean<>(new ArrayList<>());
	}

	@RequestMapping(path = "/definitions/load/params/{itemId}")
	public ResultBean<GoodsAttributeDTO> loadDefinitions(@PathVariable("itemId") Long itemId, @RequestBody KeyDTO<Long> dto) {
		return new ResultBean<>(service.loadDefinitions(itemId, dto));
	}

	@RequestMapping(path = "/load/params/{id}")
	public ResultBean<GoodsItemDTO> load(@PathVariable("id") Long id) {
		GoodsItemDTO dto = service.load(ClientContext.getMerchantId(), id);
		GoodsItemEvaluationDTO evaDto = evaService.getTopEvaluation(ClientContext.getMerchantId(), dto.getId());
		dto.setTopEvaluation(evaDto);
		dto.setTotalEvaluations(evaService.getTotalEvaluation(ClientContext.getMerchantId(), dto.getId()));
		return new ResultBean<>(dto);
	}

	@RequestMapping(path = "/{itemId}/{index}/{userId}/poster/share.png", produces = { MediaType.IMAGE_PNG_VALUE })
	public Resource loadInvitePoster(@PathVariable("itemId") Long itemId, @PathVariable("index") int index, @PathVariable("userId") Long userId)
			throws Exception {
		return new ByteArrayResource(service.loadSharePoster(itemId, index, userId));
	}
}
