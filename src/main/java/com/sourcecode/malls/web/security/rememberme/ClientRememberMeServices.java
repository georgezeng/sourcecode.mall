package com.sourcecode.malls.web.security.rememberme;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;
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
		if (request.getHeader("Origin") != null) {
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
	}

	@Override
	protected UserDetails processAutoLoginCookie(String[] cookieTokens, HttpServletRequest request, HttpServletResponse response) {
		if (cookieTokens.length != 3) {
			throw new InvalidCookieException("Cookie token did not contain 3" + " tokens, but contained '" + Arrays.asList(cookieTokens) + "'");
		}

		long tokenExpiryTime;

		try {
			tokenExpiryTime = new Long(cookieTokens[1]).longValue();
		} catch (NumberFormatException nfe) {
			throw new InvalidCookieException("Cookie token[1] did not contain a valid number (contained '" + cookieTokens[1] + "')");
		}

		if (isTokenExpired(tokenExpiryTime)) {
			throw new InvalidCookieException(
					"Cookie token[1] has expired (expired on '" + new Date(tokenExpiryTime) + "'; current time is '" + new Date() + "')");
		}

		// Check the user exists.
		// Defer lookup until after expiry time checked, to possibly avoid expensive
		// database call.

		try {
			setMerchantId(request);
			if (ClientContext.getMerchantId() == null) {
				throw new RememberMeAuthenticationException("没有商家信息");
			}
			UserDetails userDetails = getUserDetailsService().loadUserByUsername(cookieTokens[0]);

			String password = userDetails.getPassword();
			if (password == null) {
				password = "";
			}

			// Check signature of token matches remaining details.
			// Must do this after user lookup, as we need the DAO-derived password.
			// If efficiency was a major issue, just add in a UserCache implementation,
			// but recall that this method is usually only called once per HttpSession - if
			// the token is valid,
			// it will cause SecurityContextHolder population, whilst if invalid, will cause
			// the cookie to be cancelled.
			String expectedTokenSignature = makeTokenSignature(tokenExpiryTime, userDetails.getUsername(), password);

			if (!equals(expectedTokenSignature, cookieTokens[2])) {
				String msg = "Cookie token[2] contained signature '" + cookieTokens[2] + "' but expected '" + expectedTokenSignature + "'";
				logger.warn(msg);
				throw new InvalidCookieException(msg);
			}
			return userDetails;
		} finally {
			ClientContext.clear();
		}

	}

	private static boolean equals(String expected, String actual) {
		byte[] expectedBytes = bytesUtf8(expected);
		byte[] actualBytes = bytesUtf8(actual);
		if (expectedBytes.length != actualBytes.length) {
			return false;
		}

		int result = 0;
		for (int i = 0; i < expectedBytes.length; i++) {
			result |= expectedBytes[i] ^ actualBytes[i];
		}
		return result == 0;
	}

	private static byte[] bytesUtf8(String s) {
		if (s == null) {
			return null;
		}
		return Utf8.encode(s);
	}

	@Override
	public void onLoginSuccess(HttpServletRequest request, HttpServletResponse response, Authentication successfulAuthentication) {
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
			try {
				setMerchantId(request);
				UserDetails user = getUserDetailsService().loadUserByUsername(username);
				password = user.getPassword();
				if (password == null) {
					password = "";
				}
			} finally {
				ClientContext.clear();
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

	}
	
	protected void onLoginFail(HttpServletRequest request, HttpServletResponse response) {
		Cookie cookie = new Cookie("token", null);
		cookie.setMaxAge(0);
		cookie.setPath("/");
		response.addCookie(cookie);
		logger.warn("Auto login failed.........");
	}

}
