package com.sourcecode.malls.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.merchant.AdvertisementSettingDTO;
import com.sourcecode.malls.dto.query.QueryInfo;
import com.sourcecode.malls.enums.AdvertisementType;
import com.sourcecode.malls.service.impl.AdvertisementService;

@RestController
@RequestMapping(path = "/advertisement")
public class AdvertisementController {

	@Autowired
	private AdvertisementService service;

	@RequestMapping(path = "/list")
	public ResultBean<AdvertisementSettingDTO> list(@RequestBody QueryInfo<AdvertisementType> queryInfo) {
		return new ResultBean<>(service.getList(ClientContext.getMerchantId(), queryInfo));
	}

}
