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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sourcecode.malls.constants.RequestParams;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.merchant.Merchant;
import com.sourcecode.malls.repository.jpa.impl.client.ClientRepository;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantRepository;

@Component
public class ClientUsernamePasswordAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

	public ClientUsernamePasswordAuthenticationFilter() {
		super(new AntPathRequestMatcher("/login", "POST"));
	}

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private MerchantRepository merchantRepository;

	@Autowired
	private PasswordEncoder encoder;

	@Override
	protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
		boolean matched = super.requiresAuthentication(request, response);
		return matched && "UsernamePassword".equals(request.getParameter(RequestParams.LOGIN_TYPE));
	}

	@Override
	public void afterPropertiesSet() {
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException, ServletException {
		String merchantIdStr = request.getHeader(RequestParams.MERCHANT_ID);
		if (StringUtils.isEmpty(merchantIdStr) || "null".equals(merchantIdStr)) {
			throw new AuthenticationServiceException("商户不存在");
		}
		String username = request.getParameter(RequestParams.USERNAME);
		String password = request.getParameter(RequestParams.PASSWORD);
		if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
			throw new AuthenticationServiceException("账号或密码有误");
		}
		Long merchantId = Long.valueOf(merchantIdStr);
		Optional<Merchant> merchant = merchantRepository.findById(merchantId);
		if (!merchant.isPresent()) {
			throw new AuthenticationServiceException("商户不存在");
		}
		Optional<Client> userOp = clientRepository.findByMerchantAndUsername(merchant.get(), username);
		if (!userOp.isPresent()) {
			throw new UsernameNotFoundException("账号或密码有误");
		}
		if (!encoder.matches(password, userOp.get().getPassword())) {
			throw new UsernameNotFoundException("账号或密码有误");
		}
		return new UsernamePasswordAuthenticationToken(userOp.get(), password, userOp.get().getAuthorities());
	}

}
