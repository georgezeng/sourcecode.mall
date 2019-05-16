package com.sourcecode.malls.web.security.rememberme;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

import com.sourcecode.malls.constants.RequestParams;
import com.sourcecode.malls.context.ClientContext;

public class ClientRememberMeServices extends TokenBasedRememberMeServices {

	public ClientRememberMeServices(String key, UserDetailsService userDetailsService) {
		super(key, userDetailsService);
	}

	@Override
	protected UserDetails processAutoLoginCookie(String[] cookieTokens, HttpServletRequest request, HttpServletResponse response) {
		try {
			ClientContext.setMerchantId(Long.valueOf(request.getParameter(RequestParams.MERCHANT_ID)));
			return super.processAutoLoginCookie(cookieTokens, request, response);
		} finally {
			ClientContext.setMerchantId(null);
		}
	}

}
