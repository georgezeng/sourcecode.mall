package com.sourcecode.malls.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.dto.ClientBonusInfo;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.merchant.SiteInfo;
import com.sourcecode.malls.service.impl.MerchantService;

@RestController
@RequestMapping(path = "/merchant")
public class MerchantController {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private MerchantService merchantService;

	@RequestMapping(path = "/siteInfo")
	public ResultBean<SiteInfo> siteInfo() throws Exception {
		return new ResultBean<>(merchantService.getSiteInfo(ClientContext.getMerchantId()));
	}

	@RequestMapping(path = "/clientBonusInfo")
	public ResultBean<ClientBonusInfo> getClientBonusInfo() throws Exception {
		return new ResultBean<>(merchantService.getBonusInfo(ClientContext.get().getMerchant()));
	}

}
