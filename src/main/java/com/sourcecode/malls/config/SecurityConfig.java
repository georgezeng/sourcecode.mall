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
	private ClientRememberMeServices rememberMeServices;

	@Override
	protected void processAuthorizations(HttpSecurity http) throws Exception {
		rememberMeServices.setAlwaysRemember(true);
		http.rememberMe().rememberMeServices(rememberMeServices);
		http.authorizeRequests().antMatchers("/client/wechat/**").permitAll();
		http.authorizeRequests().anyRequest().authenticated();
	}

	@Override
	protected void after(HttpSecurity http) throws Exception {
		verifyCodeAuthenticationFilter.setRememberMeServices(rememberMeServices);
		verifyCodeAuthenticationFilter.setAuthenticationSuccessHandler(successHandler);
		verifyCodeAuthenticationFilter.setAuthenticationFailureHandler(failureHandler);
		authenticationFilter.setRememberMeServices(rememberMeServices);
		authenticationFilter.setAuthenticationSuccessHandler(successHandler);
		authenticationFilter.setAuthenticationFailureHandler(failureHandler);
		wechatAuthenticationFilter.setRememberMeServices(rememberMeServices);
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
