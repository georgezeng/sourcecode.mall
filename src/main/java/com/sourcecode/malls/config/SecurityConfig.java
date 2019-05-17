package com.sourcecode.malls.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.sourcecode.malls.constants.RequestParams;
import com.sourcecode.malls.service.impl.ClientService;
import com.sourcecode.malls.web.security.filter.ClientSessionFilter;
import com.sourcecode.malls.web.security.filter.ClientUsernamePasswordAuthenticationFilter;
import com.sourcecode.malls.web.security.filter.ClientVerifyCodeAuthenticationFilter;
import com.sourcecode.malls.web.security.rememberme.ClientRememberMeServices;

@Configuration
public class SecurityConfig extends BaseSecurityConfig {

	@Autowired
	private ClientSessionFilter sessionFilter;

	@Autowired
	private ClientVerifyCodeAuthenticationFilter registerAuthenticationFilter;

	@Autowired
	private ClientUsernamePasswordAuthenticationFilter authenticationFilter;

	@Autowired
	private ClientService clientService;

	@Override
	protected List<String> getAllowHeaders() {
		return Arrays.asList("Access-Control-Allow-Origin", "Access-Control-Request-Method", "Content-Type", RequestParams.MERCHANT_ID);
	}

	@Override
	protected void processAuthorizations(HttpSecurity http) throws Exception {
		http.authorizeRequests().antMatchers("/client/login/**").permitAll();
		http.authorizeRequests().anyRequest().authenticated();
		http.rememberMe().rememberMeServices(new ClientRememberMeServices("Client_Remember_Key", clientService));
	}

	@Override
	protected void after(HttpSecurity http) throws Exception {
		registerAuthenticationFilter.setAuthenticationSuccessHandler(successHandler);
		registerAuthenticationFilter.setAuthenticationFailureHandler(failureHandler);
		authenticationFilter.setAuthenticationSuccessHandler(successHandler);
		authenticationFilter.setAuthenticationFailureHandler(failureHandler);
		http.addFilterBefore(sessionFilter, FilterSecurityInterceptor.class);
		http.addFilterBefore(registerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilterAt(authenticationFilter, UsernamePasswordAuthenticationFilter.class);
	}

	protected UserDetailsService getUserDetailsService() {
		return clientService;
	}
}
