package com.sourcecode.malls.web.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sourcecode.malls.domain.merchant.Merchant;
import com.sourcecode.malls.domain.merchant.MerchantSetting;
import com.sourcecode.malls.domain.system.User;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantRepository;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantSettingRepository;
import com.sourcecode.malls.web.controller.base.BaseController;

@RestController
@RequestMapping(path = "/merchant/setting")
public class MerchantSettingController extends BaseController {

	@Autowired
	private MerchantRepository merchantRepository;

	@Autowired
	private MerchantSettingRepository merchantSettingRepository;

	@RequestMapping(path = "/load/params/{code}")
	public ResultBean<String> load(@PathVariable String code) {
		User user = getRelatedCurrentUser();
		Optional<Merchant> merchant = merchantRepository.findById(user.getId());
		Optional<MerchantSetting> setting = merchantSettingRepository.findByMerchantAndCode(merchant.get(), code);
		MerchantSetting data = setting.orElseGet(MerchantSetting::new);
		return new ResultBean<>(data.getValue());
	}

}
