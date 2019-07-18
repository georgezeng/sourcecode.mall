package com.sourcecode.malls.web.controller;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sourcecode.malls.constants.EnvConstant;
import com.sourcecode.malls.constants.ExceptionMessageConstant;
import com.sourcecode.malls.constants.SystemConstant;
import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.client.ClientIdentity;
import com.sourcecode.malls.domain.redis.CodeStore;
import com.sourcecode.malls.dto.PasswordDTO;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.client.ClientDTO;
import com.sourcecode.malls.dto.client.ClientIdentityDTO;
import com.sourcecode.malls.enums.VerificationStatus;
import com.sourcecode.malls.repository.jpa.impl.client.ClientIdentityRepository;
import com.sourcecode.malls.repository.redis.impl.CodeStoreRepository;
import com.sourcecode.malls.service.FileOnlineSystemService;
import com.sourcecode.malls.service.impl.ClientService;
import com.sourcecode.malls.service.impl.VerifyCodeService;
import com.sourcecode.malls.util.AssertUtil;
import com.sourcecode.malls.util.RegexpUtil;

@RestController
@RequestMapping(path = "/client")
public class ClientController {
	Logger logger = LoggerFactory.getLogger(getClass());

	private static final String FORGET_PASSWORD_CATEGORY = "forget-password-category";

	@Autowired
	private VerifyCodeService verifyCodeService;

	@Autowired
	private ClientService clientService;

	@Autowired
	private CodeStoreRepository codeStoreRepository;

	@Autowired
	private ClientIdentityRepository identityRepository;

	@Autowired
	private PasswordEncoder encoder;

	@Autowired
	private FileOnlineSystemService fileService;

	@Value("${user.type.name}")
	private String userDir;

	@Autowired
	private Environment env;

	@RequestMapping(path = "/img/load", produces = { MediaType.IMAGE_PNG_VALUE })
	public Resource loadImg(@RequestParam(name = "filePath") String filePath) {
		Client client = ClientContext.get();
		String rootPath = userDir + "/" + client.getId() + "/";
		AssertUtil.assertTrue(filePath.startsWith(rootPath),
				ExceptionMessageConstant.FILE_PATH_IS_INVALID + ": " + filePath);
		return new ByteArrayResource(fileService.load(false, filePath));
	}

	@RequestMapping(value = "/avatar/upload")
	public ResultBean<String> uploadAvatar(@RequestParam("file") MultipartFile file) throws IOException {
		String extend = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
		String filePath = userDir + "/" + ClientContext.get().getId();
		if (env.acceptsProfiles(Profiles.of(EnvConstant.LOCAL))) {
			filePath += "/avatar_" + System.currentTimeMillis() + extend;
		} else {
			filePath += "/avatar" + extend;
		}
		fileService.upload(true, filePath, file.getInputStream());
		Client client = ClientContext.get();
		client.setAvatar(filePath);
		clientService.save(client);
		return new ResultBean<>(filePath);
	}

	@RequestMapping(value = "/identity/upload")
	public ResultBean<String> uploadIdentity(@RequestParam("file") MultipartFile file) throws IOException {
		String filePath = userDir + "/" + ClientContext.get().getId() + "/identity/" + file.getOriginalFilename();
		fileService.upload(false, filePath, file.getInputStream());
		return new ResultBean<>(filePath);
	}

	@RequestMapping(path = "/save")
	public ResultBean<Void> save(@RequestBody ClientDTO dto) {
		Client client = ClientContext.get();
		client.setBirthday(dto.getBirthday());
		client.setSex(dto.getSex());
		client.setNickname(dto.getNickname());
		client.setAvatar(dto.getAvatar());
		clientService.save(client);
		return new ResultBean<>();
	}

	@RequestMapping(path = "/identity/load")
	public ResultBean<ClientIdentityDTO> loadImg() {
		Client client = ClientContext.get();
		ClientIdentity data = identityRepository.findByClient(client).orElse(null);
		ClientIdentityDTO dto = null;
		if (data != null) {
			dto = data.asDTO();
		}
		return new ResultBean<>(dto);
	}

	@RequestMapping(path = "/identity/save")
	public ResultBean<Void> saveIdentity(@RequestBody ClientIdentityDTO identity) {
		Client client = ClientContext.get();
		ClientIdentity data = identityRepository.findByClient(client).orElseGet(ClientIdentity::new);
		AssertUtil.assertTrue(!VerificationStatus.Passed.equals(data.getStatus()), "已经审核通过，不需要再次提交");
		BeanUtils.copyProperties(identity, data, "id", "merchantId", "status", "reason");
		if (data.getId() == null) {
			data.setClient(client);
		}
		data.setStatus(VerificationStatus.Checking);
		data.setReason(null);
		identityRepository.save(data);
		return new ResultBean<>();
	}

	@RequestMapping(path = "/current")
	public ResultBean<ClientDTO> current() {
		ClientDTO client = ClientContext.get().asDTO();
		return new ResultBean<>(client);
	}

	@RequestMapping(path = "/resetPassword/code")
	public ResultBean<Void> sendResetPasswordCode() {
		sendForgetPasswordCode(ClientContext.get().getUsername());
		return new ResultBean<>();
	}

	@RequestMapping(path = "/resetPassword")
	public ResultBean<Void> resetPassword(@RequestBody PasswordDTO dto) {
		Optional<CodeStore> codeStoreOp = codeStoreRepository.findByCategoryAndKey(FORGET_PASSWORD_CATEGORY,
				ClientContext.get().getUsername() + "_" + ClientContext.getMerchantId());
		AssertUtil.assertTrue(codeStoreOp.isPresent(), ExceptionMessageConstant.VERIFY_CODE_INVALID);
		AssertUtil.assertTrue(codeStoreOp.get().getValue().equals(dto.getOldPassword()),
				ExceptionMessageConstant.VERIFY_CODE_INVALID);
		AssertUtil.assertTrue(RegexpUtil.matchPassword(dto.getPassword()),
				ExceptionMessageConstant.PASSWORD_SHOULD_BE_THE_RULE);
		AssertUtil.assertTrue(dto.getPassword().equals(dto.getConfirmPassword()),
				ExceptionMessageConstant.TWO_TIMES_PASSWORD_NOT_EQUALS);
		Client user = ClientContext.get();
		user.setPassword(encoder.encode(dto.getPassword()));
		clientService.save(user);
		codeStoreRepository.delete(codeStoreOp.get());
		return new ResultBean<>();
	}

	@RequestMapping(path = "/updatePassword")
	public ResultBean<Void> updatePassword(@RequestBody PasswordDTO dto) {
		Client user = ClientContext.get();
		AssertUtil.assertNotEmpty(user.getPassword(), "还没有设置过密码，请使用重置密码功能");
		AssertUtil.assertTrue(encoder.matches(dto.getOldPassword(), user.getPassword()),
				ExceptionMessageConstant.OLD_PASSWORD_IS_INVALID);
		AssertUtil.assertTrue(RegexpUtil.matchPassword(dto.getPassword()),
				ExceptionMessageConstant.PASSWORD_SHOULD_BE_THE_RULE);
		AssertUtil.assertTrue(dto.getPassword().equals(dto.getConfirmPassword()),
				ExceptionMessageConstant.TWO_TIMES_PASSWORD_NOT_EQUALS);
		user.setPassword(encoder.encode(dto.getPassword()));
		clientService.save(user);
		return new ResultBean<>();
	}

	@RequestMapping(path = "/login/code/{mobile}")
	public ResultBean<Void> sendLoginVerifyCode(@PathVariable String mobile) {
		verifyCodeService.sendLoginCode(mobile, SystemConstant.LOGIN_VERIFY_CODE_CATEGORY,
				ClientContext.getMerchantId() + "");
		return new ResultBean<>();
	}

	@RequestMapping(path = "/forgetPassword/code/{mobile}")
	public ResultBean<Void> sendForgetPasswordCode(@PathVariable String mobile) {
		clientService.findByMerchantAndUsername(ClientContext.getMerchantId(), mobile);
		verifyCodeService.sendForgetPasswordCode(mobile, FORGET_PASSWORD_CATEGORY, ClientContext.getMerchantId() + "");
		return new ResultBean<>();
	}

	@RequestMapping(path = "/forgetPassword/check/{mobile}/{code}")
	public ResultBean<Void> checkCodeForForgetPassword(@PathVariable String mobile, @PathVariable String code) {
		Optional<CodeStore> codeStoreOp = codeStoreRepository.findByCategoryAndKey(FORGET_PASSWORD_CATEGORY,
				mobile + "_" + ClientContext.getMerchantId());
		AssertUtil.assertTrue(codeStoreOp.isPresent(), ExceptionMessageConstant.VERIFY_CODE_INVALID);
		AssertUtil.assertTrue(codeStoreOp.get().getValue().equals(code), ExceptionMessageConstant.VERIFY_CODE_INVALID);
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
