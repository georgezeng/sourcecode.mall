package com.sourcecode.malls.web.security.filter;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import com.alibaba.druid.util.StringUtils;
import com.sourcecode.malls.constants.RequestParams;
import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.merchant.MerchantShopApplication;
import com.sourcecode.malls.exception.BusinessException;
import com.sourcecode.malls.properties.SessionAttributesProperties;
import com.sourcecode.malls.repository.jpa.impl.client.ClientRepository;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantShopApplicationRepository;
import com.sourcecode.malls.service.impl.ClientService;

@Component
public class ClientSessionFilter extends GenericFilterBean {

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private MerchantShopApplicationRepository applicationRepository;

	@Autowired
	private ClientService clientService;

	@Autowired
	private SessionAttributesProperties sessionProperties;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		try {
			HttpSession session = ((HttpServletRequest) request).getSession();
			Long userId = (Long) session.getAttribute(sessionProperties.getUserId());
			if (userId != null) {
				Optional<Client> user = clientRepository.findById(userId);
				if (user.isPresent()) {
					ClientContext.set(user.get());
					ClientContext.setMerchantId(user.get().getMerchant().getId());
				}
			} else if (SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
				HttpServletRequest httpReq = (HttpServletRequest) request;
				String domain = httpReq.getHeader("Origin").replaceAll("http(s?)://", "").replaceAll("/.*", "");
				if (StringUtils.isEmpty(domain)) {
					throw new AuthenticationServiceException("商户不存在");
				}
				Optional<MerchantShopApplication> apOp = applicationRepository.findByDomain(domain);
				if (apOp.isPresent()) {
					Long merchantId = apOp.get().getMerchant().getId();
					Authentication token = SecurityContextHolder.getContext().getAuthentication();
					if (RememberMeAuthenticationToken.class.isAssignableFrom(token.getClass())) {
						RememberMeAuthenticationToken rToken = (RememberMeAuthenticationToken) token;
						UserDetails details = (UserDetails) rToken.getPrincipal();
						Client client = clientService.findByMerchantAndUsername(merchantId, details.getUsername());
						ClientContext.set(client);
						ClientContext.setMerchantId(merchantId);
						session.setAttribute(sessionProperties.getUserId(), client.getId());
					} else {
						ClientContext.setMerchantId(merchantId);
					}
				}
			} else {
				throw new BusinessException("用户登录状态有误");
			}
			chain.doFilter(request, response);
		} finally {
			ClientContext.set(null);
			ClientContext.setMerchantId(null);
		}
	}

}
