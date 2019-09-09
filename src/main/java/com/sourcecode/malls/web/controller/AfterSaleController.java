package com.sourcecode.malls.web.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.dto.aftersale.AfterSaleApplicationDTO;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.client.ClientAddressDTO;
import com.sourcecode.malls.dto.order.ExpressDTO;
import com.sourcecode.malls.dto.query.PageInfo;
import com.sourcecode.malls.enums.AfterSaleType;
import com.sourcecode.malls.service.FileOnlineSystemService;
import com.sourcecode.malls.service.impl.AfterSaleService;

@RestController
@RequestMapping(path = "/afterSale")
public class AfterSaleController {

	@Autowired
	private AfterSaleService service;

	@Autowired
	private FileOnlineSystemService fileService;

	@Value("${user.type.name}")
	private String userDir;

	@RequestMapping(path = "/reason/list/params/{type}")
	public ResultBean<String> listReasons(@PathVariable AfterSaleType type) {
		return new ResultBean<>(service.getAllReasons(ClientContext.get().getMerchant(), type));
	}

	@RequestMapping(path = "/list/params/{id}/{status}")
	public ResultBean<AfterSaleApplicationDTO> list(@PathVariable("id") Long id, @PathVariable("status") String status,
			@RequestBody PageInfo pageInfo) {
		return new ResultBean<>(service.list(ClientContext.get(), id, status, pageInfo));
	}

	@RequestMapping(value = "/upload/params/{id}")
	public ResultBean<String> upload(@PathVariable Long id, @RequestParam("files") List<MultipartFile> files) throws IOException {
		List<String> filePaths = new ArrayList<>();
		for (MultipartFile file : files) {
			String extend = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
			String filePath = userDir + "/" + ClientContext.get().getId() + "/sub_order/" + id + "/aftersale/" + System.currentTimeMillis() + extend;
			fileService.upload(true, filePath, file.getInputStream());
			filePaths.add(filePath);
		}
		return new ResultBean<>(filePaths);
	}

	@RequestMapping(value = "/apply/refund")
	public ResultBean<Void> applyRefund(@RequestBody AfterSaleApplicationDTO dto) {
		service.applyRefund(ClientContext.get().getId(), dto);
		return new ResultBean<>();
	}

	@RequestMapping(value = "/apply")
	public ResultBean<Void> apply(@RequestBody AfterSaleApplicationDTO dto) {
		service.apply(ClientContext.get().getId(), dto);
		return new ResultBean<>();
	}

	@RequestMapping(value = "/returnAddress/load/params/{id}")
	public ResultBean<ClientAddressDTO> returnAddress(@PathVariable Long id) throws Exception {
		AfterSaleApplicationDTO data = service.load(ClientContext.get().getId(), id);
		return new ResultBean<>(data.getReturnAddress());
	}

	@RequestMapping(value = "/fillExpress")
	public ResultBean<Void> fillExpress(@RequestBody ExpressDTO dto) {
		service.fillExpress(dto);
		return new ResultBean<>();
	}

	@RequestMapping(value = "/pickup/params/{id}")
	public ResultBean<Void> pickup(@PathVariable Long id) {
		service.pickup(id);
		return new ResultBean<>();
	}

	@RequestMapping(value = "/count/unFinished")
	public ResultBean<Long> countUnFinished() {
		return new ResultBean<>(service.countUnFinished(ClientContext.get()));
	}

	@RequestMapping(value = "/load/params/{id}")
	public ResultBean<AfterSaleApplicationDTO> load(@PathVariable Long id) {
		return new ResultBean<>(service.load(ClientContext.get().getId(), id));
	}

}
