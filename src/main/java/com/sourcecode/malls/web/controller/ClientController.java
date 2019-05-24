package com.sourcecode.malls.web.controller;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sourcecode.malls.constants.SystemConstant;
import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.redis.CodeStore;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.client.ClientDTO;
import com.sourcecode.malls.repository.redis.impl.CodeStoreRepository;
import com.sourcecode.malls.service.FileOnlineSystemService;
import com.sourcecode.malls.service.impl.ClientService;
import com.sourcecode.malls.service.impl.VerifyCodeService;
import com.sourcecode.malls.util.AssertUtil;

@RestController
@RequestMapping(path = "/client")
public class ClientController {
	Logger logger = LoggerFactory.getLogger(getClass());

	private static final String LOGIN_CODE_TIME_ATTR = "login-register-code-time";
	private static final String FORGET_PASSWORD_TIME_ATTR = "forget-password-code-time";
	private static final String FORGET_PASSWORD_CATEGORY = "forget-password-category";

	@Autowired
	private VerifyCodeService verifyCodeService;

	@Autowired
	private ClientService clientService;

	@Autowired
	private CodeStoreRepository codeStoreRepository;

	@Autowired
	private PasswordEncoder encoder;

	@Autowired
	private FileOnlineSystemService fileService;

	@Value("${user.type.name}")
	private String userDir;

	@RequestMapping(path = "/img/load")
	public Resource loadImg(@RequestParam String filePath) {
		Client client = ClientContext.get();
		String path = userDir + "/" + client.getId() + "/" + filePath;
		return new ByteArrayResource(fileService.load(false, path));
	}

	@RequestMapping(path = "/current")
	public ResultBean<ClientDTO> current() {
		ClientDTO client = ClientContext.get().asDTO();
		return new ResultBean<>(client);
	}

	@RequestMapping(path = "/login/code/{mobile}")
	public ResultBean<Void> sendLoginVerifyCode(@PathVariable String mobile) {
		verifyCodeService.sendLoginCode(mobile, LOGIN_CODE_TIME_ATTR, SystemConstant.LOGIN_VERIFY_CODE_CATEGORY, ClientContext.getMerchantId() + "");
		return new ResultBean<>();
	}

	@RequestMapping(path = "/forgetPassword/code/{mobile}")
	public ResultBean<Void> sendForgetPasswordCode(@PathVariable String mobile) {
		clientService.findByMerchantAndUsername(ClientContext.getMerchantId(), mobile);
		verifyCodeService.sendForgetPasswordCode(mobile, FORGET_PASSWORD_TIME_ATTR, FORGET_PASSWORD_CATEGORY, ClientContext.getMerchantId() + "");
		return new ResultBean<>();
	}

	@RequestMapping(path = "/forgetPassword/check/{mobile}/{code}")
	public ResultBean<Void> checkCodeForForgetPassword(@PathVariable String mobile, @PathVariable String code) {
		Optional<CodeStore> codeStoreOp = codeStoreRepository.findByCategoryAndKey(FORGET_PASSWORD_CATEGORY,
				mobile + "_" + ClientContext.getMerchantId());
		AssertUtil.assertTrue(codeStoreOp.isPresent(), "验证码无效");
		AssertUtil.assertTrue(codeStoreOp.get().getValue().equals(code), "验证码无效");
		return new ResultBean<>();
	}

	@RequestMapping(path = "/forgetPassword/reset/{code}")
	public ResultBean<Void> checkCodeForForgetPassword(@RequestBody Client client, @PathVariable String code) {
		checkCodeForForgetPassword(client.getUsername(), code);
		Client user = clientService.findByMerchantAndUsername(ClientContext.getMerchantId(), client.getUsername());
		user.setPassword(encoder.encode(client.getPassword()));
		clientService.save(user);
		return new ResultBean<>();
	}

}
