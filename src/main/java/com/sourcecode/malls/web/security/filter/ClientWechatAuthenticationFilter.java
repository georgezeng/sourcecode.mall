package com.sourcecode.malls.web.security.filter;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
import com.sourcecode.malls.constants.SessionAttributes;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.merchant.Merchant;
import com.sourcecode.malls.domain.merchant.MerchantShopApplication;
import com.sourcecode.malls.repository.jpa.impl.client.ClientRepository;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantShopApplicationRepository;

@Component
public class ClientWechatAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private MerchantShopApplicationRepository applicationRepository;

	public ClientWechatAuthenticationFilter() {
		super(new AntPathRequestMatcher("/login", "POST"));
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException, ServletException {
		String domain = request.getHeader("Origin").replaceAll("http(s?)://", "").replaceAll("/.*", "");
		if (StringUtils.isEmpty(domain)) {
			throw new AuthenticationServiceException("商户不存在");
		}
		Optional<MerchantShopApplication> apOp = applicationRepository.findByDomain(domain);
		if (!apOp.isPresent()) {
			throw new AuthenticationServiceException("商户不存在");
		}
		Merchant merchant = apOp.get().getMerchant();
		String username = request.getParameter(RequestParams.USERNAME);
		String password = request.getParameter(RequestParams.PASSWORD);
		if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
			throw new AuthenticationServiceException("登录参数有误");
		}
		HttpSession session = request.getSession();
		String token = (String) session.getAttribute(SessionAttributes.LOGIN_TOKEN);
		if (!password.equals(token)) {
			throw new AuthenticationServiceException("账号或密码有误");
		}
		Optional<Client> userOp = clientRepository.findByMerchantAndUsername(merchant, username);
		if (!userOp.isPresent()) {
			throw new AuthenticationServiceException("账号或密码有误");
		}
		Client user = userOp.get();
		return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
	}

	@Override
	protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
		boolean matched = super.requiresAuthentication(request, response);
		return matched && "ThirdParty".equals(request.getParameter(RequestParams.LOGIN_TYPE));
	}

	@Override
	public void afterPropertiesSet() {
	}

}