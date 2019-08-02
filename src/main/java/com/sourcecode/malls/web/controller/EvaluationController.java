package com.sourcecode.malls.web.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sourcecode.malls.constants.ExceptionMessageConstant;
import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.goods.GoodsItemEvaluation;
import com.sourcecode.malls.domain.order.SubOrder;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.goods.GoodsItemEvaluationDTO;
import com.sourcecode.malls.dto.order.SubOrderDTO;
import com.sourcecode.malls.dto.query.QueryInfo;
import com.sourcecode.malls.repository.jpa.impl.goods.GoodsItemEvaluationRepository;
import com.sourcecode.malls.repository.jpa.impl.order.SubOrderRepository;
import com.sourcecode.malls.service.FileOnlineSystemService;
import com.sourcecode.malls.service.impl.EvaluationService;
import com.sourcecode.malls.util.AssertUtil;

@RestController
@RequestMapping(path = "/evaluation")
public class EvaluationController {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private EvaluationService service;

	@Autowired
	private SubOrderRepository subOrderRepository;

	@Autowired
	private GoodsItemEvaluationRepository repository;

	@Autowired
	private FileOnlineSystemService fileService;

	@Value("${user.type.name}")
	private String userDir;

	@RequestMapping(value = "/upload")
	public ResultBean<String> upload(@RequestParam("files") List<MultipartFile> files) throws IOException {
		List<String> filePaths = new ArrayList<>();
		for (MultipartFile file : files) {
			String extend = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
			String filePath = userDir + "/" + ClientContext.get().getId() + "/evaluation/" + System.currentTimeMillis()
					+ extend;
			fileService.upload(true, filePath, file.getInputStream());
			filePaths.add(filePath);
		}
		return new ResultBean<>(filePaths);
	}

	@RequestMapping(path = "/uncomment/list")
	public ResultBean<SubOrderDTO> listUnComment(@RequestBody QueryInfo<Long> queryInfo) {
		return new ResultBean<>(service.getUnCommentList(ClientContext.get(), queryInfo).getList());
	}

	@RequestMapping(path = "/uncomment/count")
	public ResultBean<Long> countUnComment(@RequestBody QueryInfo<Long> queryInfo) {
		queryInfo.getPage().setSize(1);
		return new ResultBean<>(service.getUnCommentList(ClientContext.get(), queryInfo).getTotal());
	}

	@RequestMapping(path = "/comment/list")
	public ResultBean<GoodsItemEvaluationDTO> listComment(@RequestBody QueryInfo<Long> queryInfo) {
		return new ResultBean<>(service.getCommentList(ClientContext.get(), queryInfo).getList());
	}

	@RequestMapping(path = "/comment/count")
	public ResultBean<Long> countComment(@RequestBody QueryInfo<Long> queryInfo) {
		queryInfo.getPage().setSize(1);
		return new ResultBean<>(service.getCommentList(ClientContext.get(), queryInfo).getTotal());
	}

	@RequestMapping(path = "/goodsItem/list")
	public ResultBean<GoodsItemEvaluationDTO> listCommentForGoodsItem(
			@RequestBody QueryInfo<GoodsItemEvaluationDTO> queryInfo) {
		return new ResultBean<>(service.getCommentListForGoodsItem(ClientContext.getMerchantId(), queryInfo).getList());
	}

	@RequestMapping(path = "/goodsItem/count")
	public ResultBean<Long> countCommentForGoodsItem(@RequestBody QueryInfo<GoodsItemEvaluationDTO> queryInfo) {
		queryInfo.getPage().setSize(1);
		return new ResultBean<>(
				service.getCommentListForGoodsItem(ClientContext.getMerchantId(), queryInfo).getTotal());
	}

	@RequestMapping(path = "/load/subOrder/params/{id}")
	public ResultBean<GoodsItemEvaluationDTO> loadSubOrder(@PathVariable Long id) {
		Optional<SubOrder> order = subOrderRepository.findById(id);
		AssertUtil.assertTrue(order.isPresent() && order.get().getClient().getId().equals(ClientContext.get().getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		GoodsItemEvaluationDTO dto = new GoodsItemEvaluationDTO();
		dto.setItemThumbnail(order.get().getThumbnail());
		return new ResultBean<>(dto);
	}

	@RequestMapping(path = "/load/params/{id}")
	public ResultBean<GoodsItemEvaluationDTO> load(@PathVariable Long id) {
		Optional<GoodsItemEvaluation> data = repository.findById(id);
		AssertUtil.assertTrue(data.isPresent() && data.get().getClient().getId().equals(ClientContext.get().getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		GoodsItemEvaluationDTO dto = data.get().asDTO(false);
		dto.setItemThumbnail(data.get().getSubOrder().getItem().getThumbnail());
		return new ResultBean<>(dto);
	}

	@RequestMapping(path = "/save")
	public ResultBean<Void> save(@RequestBody GoodsItemEvaluationDTO dto) {
		service.save(ClientContext.get(), dto);
		return new ResultBean<>();
	}

	@RequestMapping(path = "/save/additional")
	public ResultBean<Void> saveAdditional(@RequestBody GoodsItemEvaluationDTO dto) {
		service.saveAdditional(ClientContext.get(), dto);
		return new ResultBean<>();
	}

}
