package com.sourcecode.malls.web.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sourcecode.malls.constants.ExceptionMessageConstant;
import com.sourcecode.malls.constants.MerchantSettingConstant;
import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.aftersale.AfterSaleApplication;
import com.sourcecode.malls.domain.merchant.Merchant;
import com.sourcecode.malls.domain.merchant.MerchantSetting;
import com.sourcecode.malls.dto.aftersale.AfterSaleApplicationDTO;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.order.ExpressDTO;
import com.sourcecode.malls.dto.query.PageInfo;
import com.sourcecode.malls.enums.AfterSaleStatus;
import com.sourcecode.malls.enums.AfterSaleType;
import com.sourcecode.malls.repository.jpa.impl.aftersale.AfterSaleApplicationRepository;
import com.sourcecode.malls.repository.jpa.impl.aftersale.AfterSaleReasonSettingRepository;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantRepository;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantSettingRepository;
import com.sourcecode.malls.service.FileOnlineSystemService;
import com.sourcecode.malls.service.impl.AfterSaleService;
import com.sourcecode.malls.util.AssertUtil;

@RestController
@RequestMapping(path = "/afterSale")
public class AfterSaleController {

	@Autowired
	private AfterSaleReasonSettingRepository settingRepository;

	@Autowired
	private AfterSaleApplicationRepository applicationRepository;

	@Autowired
	private AfterSaleService service;

//	@Autowired
//	private GoodsItemService itemService;

	@Autowired
	private MerchantRepository merchantRepository;

	@Autowired
	private MerchantSettingRepository merchantSettingRepository;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private FileOnlineSystemService fileService;

	@Value("${user.type.name}")
	private String userDir;

	@RequestMapping(path = "/reason/list/params/{type}")
	public ResultBean<String> listReasons(@PathVariable AfterSaleType type) {
		return new ResultBean<>(settingRepository.findAllByMerchantAndType(ClientContext.get().getMerchant(), type)
				.stream().map(it -> it.getContent()).collect(Collectors.toList()));
	}

	@RequestMapping(path = "/list/params/{id}/{status}")
	public ResultBean<AfterSaleApplicationDTO> list(@PathVariable("id") Long id, @PathVariable("status") String status,
			@RequestBody PageInfo pageInfo) {
		return new ResultBean<>(service.list(ClientContext.get(), id, status, pageInfo));
	}

	@RequestMapping(value = "/upload/params/{id}")
	public ResultBean<String> upload(@PathVariable Long id, @RequestParam("files") List<MultipartFile> files)
			throws IOException {
		List<String> filePaths = new ArrayList<>();
		for (MultipartFile file : files) {
			String extend = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
			String filePath = userDir + "/" + ClientContext.get().getId() + "/sub_order/" + id + "/aftersale/"
					+ System.currentTimeMillis() + extend;
			fileService.upload(true, filePath, file.getInputStream());
			filePaths.add(filePath);
		}
		return new ResultBean<>(filePaths);
	}

	@RequestMapping(value = "/apply")
	public ResultBean<Void> apply(@RequestBody AfterSaleApplicationDTO dto) {
		service.applyAfterSale(dto);
		return new ResultBean<>();
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/returnAddress/load")
	public ResultBean<Map<String, String>> returnAddress() throws Exception {
		Optional<Merchant> merchant = merchantRepository.findById(ClientContext.getMerchantId());
		Optional<MerchantSetting> setting = merchantSettingRepository.findByMerchantAndCode(merchant.get(),
				MerchantSettingConstant.RETURN_ADDRESS);
		Map<String, String> map = null;
		if (setting.isPresent()) {
			map = mapper.readValue(setting.get().getCode(), Map.class);
		}
		return new ResultBean<>(map);
	}

	@RequestMapping(value = "/fillExpress")
	public ResultBean<Void> fillExpress(@RequestBody ExpressDTO dto) {
		AssertUtil.assertNotEmpty(dto.getCompany(), "物流公司不能为空");
		AssertUtil.assertNotEmpty(dto.getNumber(), "物流单号不能为空");
		Optional<AfterSaleApplication> dataOp = applicationRepository.findById(dto.getId());
		AssertUtil.assertTrue(
				dataOp.isPresent() && dataOp.get().getClient().getId().equals(ClientContext.get().getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		AfterSaleApplication data = dataOp.get();
		AssertUtil.assertTrue(
				AfterSaleType.SalesReturn.equals(data.getType()) || AfterSaleType.Change.equals(data.getType()),
				"售后类型不是退换货服务，不能提交运单信息");
		AssertUtil.assertTrue(AfterSaleStatus.WaitForReturn.equals(data.getStatus()), "售后状态有误，不允许提交运单信息");
		data.setClientExpressCompany(dto.getCompany());
		data.setClientExpressNumber(dto.getNumber());
		data.setStatus(AfterSaleStatus.WaitForReceive);
		data.setReturnTime(new Date());
		applicationRepository.save(data);
		return new ResultBean<>();
	}

	@RequestMapping(value = "/pickup/params/{id}")
	public ResultBean<Void> pickup(@PathVariable Long id) {
		Optional<AfterSaleApplication> dataOp = applicationRepository.findById(id);
		AssertUtil.assertTrue(
				dataOp.isPresent() && dataOp.get().getClient().getId().equals(ClientContext.get().getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		AfterSaleApplication data = dataOp.get();
		AssertUtil.assertTrue(AfterSaleStatus.WaitForPickup.equals(data.getStatus()), "售后记录状态有误，不能确认收货");
		data.setStatus(AfterSaleStatus.Finished);
		data.setPickupTime(new Date());
		applicationRepository.save(data);
		return new ResultBean<>();
	}

	@RequestMapping(value = "/load/params/{id}")
	public ResultBean<AfterSaleApplicationDTO> load(@PathVariable Long id) {
		Optional<AfterSaleApplication> data = applicationRepository.findById(id);
		AssertUtil.assertTrue(data.isPresent() && data.get().getClient().getId().equals(ClientContext.get().getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		AfterSaleApplicationDTO dto = data.get().asDTO();
//		GoodsItemDTO item = itemService.load(data.get().getMerchant().getId(), data.get().getSubOrder().getItemId());
//		dto.setItem(item);
		return new ResultBean<>(dto);
	}

}