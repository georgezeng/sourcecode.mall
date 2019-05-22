package com.sourcecode.malls.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.sourcecode.malls.service.impl.ClientService;
import com.sourcecode.malls.web.security.filter.ClientSessionFilter;
import com.sourcecode.malls.web.security.filter.ClientUsernamePasswordAuthenticationFilter;
import com.sourcecode.malls.web.security.filter.ClientVerifyCodeAuthenticationFilter;
import com.sourcecode.malls.web.security.filter.ClientWechatAuthenticationFilter;
import com.sourcecode.malls.web.security.rememberme.ClientRememberMeServices;

@Configuration
public class SecurityConfig extends BaseSecurityConfig {

	@Autowired
	private ClientSessionFilter sessionFilter;

	@Autowired
	private ClientVerifyCodeAuthenticationFilter verifyCodeAuthenticationFilter;

	@Autowired
	private ClientUsernamePasswordAuthenticationFilter authenticationFilter;

	@Autowired
	private ClientWechatAuthenticationFilter wechatAuthenticationFilter;

	@Autowired
	private ClientService clientService;

	@Autowired
	private ClientRememberMeServices rememberServices;

	@Override
	protected void processAuthorizations(HttpSecurity http) throws Exception {
		http.authorizeRequests().antMatchers("/client/wechat/**").permitAll();
		http.authorizeRequests().anyRequest().authenticated();
		http.rememberMe().rememberMeServices(rememberServices);
	}

	@Override
	protected void after(HttpSecurity http) throws Exception {
		verifyCodeAuthenticationFilter.setAuthenticationSuccessHandler(successHandler);
		verifyCodeAuthenticationFilter.setAuthenticationFailureHandler(failureHandler);
		authenticationFilter.setAuthenticationSuccessHandler(successHandler);
		authenticationFilter.setAuthenticationFailureHandler(failureHandler);
		wechatAuthenticationFilter.setAuthenticationSuccessHandler(successHandler);
		wechatAuthenticationFilter.setAuthenticationFailureHandler(failureHandler);
		http.addFilterBefore(sessionFilter, FilterSecurityInterceptor.class);
		http.addFilterBefore(verifyCodeAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilterAt(authenticationFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilterAfter(wechatAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
	}

	protected UserDetailsService getUserDetailsService() {
		return clientService;
	}
}
