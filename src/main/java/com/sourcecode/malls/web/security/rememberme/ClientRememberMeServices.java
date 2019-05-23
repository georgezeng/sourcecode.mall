package com.sourcecode.malls.web.security.rememberme;

import java.util.Date;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.merchant.Merchant;
import com.sourcecode.malls.domain.merchant.MerchantShopApplication;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantShopApplicationRepository;
import com.sourcecode.malls.service.impl.ClientService;

@Component
public class ClientRememberMeServices extends TokenBasedRememberMeServices {

	@Autowired
	private MerchantShopApplicationRepository applicationRepository;

	@Autowired
	public ClientRememberMeServices(ClientService clientService) {
		super("Client_Remember_Key", clientService);
	}

	private void setMerchantId(HttpServletRequest request) {
		String domain = request.getHeader("Origin").replaceAll("http(s?)://", "").replaceAll("/.*", "");
		if (StringUtils.isEmpty(domain)) {
			throw new AuthenticationServiceException("商户不存在");
		}
		Optional<MerchantShopApplication> apOp = applicationRepository.findByDomain(domain);
		if (!apOp.isPresent()) {
			throw new AuthenticationServiceException("商户不存在");
		}
		Merchant merchant = apOp.get().getMerchant();
		ClientContext.setMerchantId(Long.valueOf(merchant.getId()));
	}

	@Override
	protected UserDetails processAutoLoginCookie(String[] cookieTokens, HttpServletRequest request, HttpServletResponse response) {
		try {
			setMerchantId(request);
			return super.processAutoLoginCookie(cookieTokens, request, response);
		} finally {
			ClientContext.clear();
		}
	}

	@Override
	public void onLoginSuccess(HttpServletRequest request, HttpServletResponse response, Authentication successfulAuthentication) {
		try {
			setMerchantId(request);
			String username = retrieveUserName(successfulAuthentication);
			String password = retrievePassword(successfulAuthentication);

			// If unable to find a username and password, just abort as
			// TokenBasedRememberMeServices is
			// unable to construct a valid token in this case.
			if (!StringUtils.hasLength(username)) {
				logger.debug("Unable to retrieve username");
				return;
			}

			if (!StringUtils.hasLength(password)) {
				UserDetails user = getUserDetailsService().loadUserByUsername(username);
				password = user.getPassword();
				if (password == null) {
					password = "";
				}
			}

			int tokenLifetime = calculateLoginLifetime(request, successfulAuthentication);
			long expiryTime = System.currentTimeMillis();
			// SEC-949
			expiryTime += 1000L * (tokenLifetime < 0 ? TWO_WEEKS_S : tokenLifetime);

			String signatureValue = makeTokenSignature(expiryTime, username, password);

			setCookie(new String[] { username, Long.toString(expiryTime), signatureValue }, tokenLifetime, request, response);

			if (logger.isDebugEnabled()) {
				logger.debug("Added remember-me cookie for user '" + username + "', expiry: '" + new Date(expiryTime) + "'");
			}
		} finally {
			ClientContext.clear();
		}
	}

}
