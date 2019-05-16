package com.sourcecode.malls.web.security.filter;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

import com.aliyuncs.utils.StringUtils;
import com.sourcecode.malls.constants.RequestParams;
import com.sourcecode.malls.constants.SystemConstant;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.merchant.Merchant;
import com.sourcecode.malls.domain.redis.CodeStore;
import com.sourcecode.malls.repository.jpa.impl.client.ClientRepository;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantRepository;
import com.sourcecode.malls.repository.redis.impl.CodeStoreRepository;
import com.sourcecode.malls.util.AssertUtil;

@Component
public class ClientVerifyCodeAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

	@Autowired
	private CodeStoreRepository codeStoreRepository;

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private MerchantRepository merchantRepository;

	public ClientVerifyCodeAuthenticationFilter() {
		super(new AntPathRequestMatcher("/login", "POST"));
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException, ServletException {
		String merchantIdStr = request.getHeader(RequestParams.MERCHANT_ID);
		if (StringUtils.isEmpty(merchantIdStr)) {
			throw new AuthenticationServiceException("商户不存在");
		}
		String username = request.getParameter(RequestParams.USERNAME);
		String verifyCode = request.getParameter(RequestParams.PASSWORD);
		if (StringUtils.isEmpty(username) || StringUtils.isEmpty(verifyCode)) {
			throw new AuthenticationServiceException("手机号或验证码有误");
		}
		Optional<CodeStore> codeStoreOp = codeStoreRepository.findByCategoryAndKey(SystemConstant.LOGIN_VERIFY_CODE_CATEGORY,
				username + "_" + merchantIdStr);
		AssertUtil.assertTrue(codeStoreOp.isPresent(), "验证码无效");
		AssertUtil.assertTrue(codeStoreOp.get().getValue().equals(verifyCode), "验证码无效");
		Long merchantId = Long.valueOf(merchantIdStr);
		Optional<Merchant> merchant = merchantRepository.findById(merchantId);
		if (!merchant.isPresent()) {
			throw new AuthenticationServiceException("商户不存在");
		}
		Optional<Client> userOp = clientRepository.findByMerchantAndUsername(merchant.get(), username);
		Client user = null;
		if (!userOp.isPresent()) {
			user = new Client();
			user.setUsername(username);
			user.setMerchant(merchant.get());
			user.setEnabled(true);
			clientRepository.save(user);
		} else {
			user = userOp.get();
		}
		return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
	}

	@Override
	protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
		boolean matched = super.requiresAuthentication(request, response);
		return matched && "PhoneVerify".equals(request.getParameter(RequestParams.LOGIN_TYPE));
	}

	@Override
	public void afterPropertiesSet() {
	}

}
