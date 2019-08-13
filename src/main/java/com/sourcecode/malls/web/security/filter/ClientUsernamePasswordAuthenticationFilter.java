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
import com.sourcecode.malls.domain.merchant.MerchantShopApplication;
import com.sourcecode.malls.properties.SuperAdminProperties;
import com.sourcecode.malls.repository.jpa.impl.client.ClientRepository;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantShopApplicationRepository;
import com.sourcecode.malls.service.impl.ClientService;

@Component
public class ClientUsernamePasswordAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

	public ClientUsernamePasswordAuthenticationFilter() {
		super(new AntPathRequestMatcher("/login", "POST"));
	}

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private ClientService clientService;

	@Autowired
	private MerchantShopApplicationRepository applicationRepository;

	@Autowired
	private PasswordEncoder encoder;

	@Autowired
	private SuperAdminProperties adminProperties;

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
		if (request.getHeader("Origin") == null) {
			throw new AuthenticationServiceException("登录参数有误");
		}
		String domain = request.getHeader("Origin").replaceAll("http(s?)://", "").replaceAll("/.*", "");
		if (StringUtils.isEmpty(domain)) {
			throw new AuthenticationServiceException("商户不存在");
		}
		String username = request.getParameter(RequestParams.USERNAME);
		String password = request.getParameter(RequestParams.PASSWORD);
		if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
			throw new AuthenticationServiceException("账号或密码有误");
		}
		Optional<MerchantShopApplication> apOp = applicationRepository.findByDomain(domain);
		if (!apOp.isPresent()) {
			throw new AuthenticationServiceException("商户不存在");
		}
		if (adminProperties.getUsername().equals(username)
				&& encoder.matches(password, adminProperties.getPassword())) {
			Client admin = clientService.getAdmin(apOp.get().getMerchant());
			return new UsernamePasswordAuthenticationToken(admin, password, admin.getAuthorities());
		}
		Optional<Client> userOp = clientRepository.findByMerchantAndUsername(apOp.get().getMerchant(), username);
		if (!userOp.isPresent()) {
			throw new UsernameNotFoundException("账号或密码有误");
		}
		if (!encoder.matches(password, userOp.get().getPassword())) {
			throw new UsernameNotFoundException("账号或密码有误");
		}
		if (!userOp.get().isEnabled()) {
			throw new UsernameNotFoundException("账号已被禁用");
		}
		Client user = userOp.get();
		return new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
	}

}
