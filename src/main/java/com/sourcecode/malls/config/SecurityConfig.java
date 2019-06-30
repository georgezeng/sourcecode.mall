package com.sourcecode.malls.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;

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
	
	private CompositeSessionAuthenticationStrategy sessionStrategy = new CompositeSessionAuthenticationStrategy(
			Arrays.asList(new ChangeSessionIdAuthenticationStrategy()));

	@Override
	protected void processAuthorizations(HttpSecurity http) throws Exception {
		rememberMeServices.setAlwaysRemember(true);
		http.rememberMe().key(rememberMeServices.getKey()).rememberMeServices(rememberMeServices);
		http.authorizeRequests().antMatchers("/goods/**").permitAll();
		http.authorizeRequests().antMatchers("/client/wechat/loginUrl").permitAll();
		http.authorizeRequests().antMatchers("/client/wechat/info").permitAll();
		http.authorizeRequests().antMatchers("/client/wechat/register").permitAll();
		http.authorizeRequests().antMatchers("/client/wechat/jsconfig").permitAll();
		http.authorizeRequests().antMatchers("/client/wechat/code/**").permitAll();
		http.authorizeRequests().antMatchers("/index").permitAll();
		http.authorizeRequests().anyRequest().authenticated();
	}

	@Override
	protected void after(HttpSecurity http) throws Exception {
		verifyCodeAuthenticationFilter.setRememberMeServices(rememberMeServices);
		verifyCodeAuthenticationFilter.setAuthenticationSuccessHandler(successHandler);
		verifyCodeAuthenticationFilter.setAuthenticationFailureHandler(failureHandler);
		verifyCodeAuthenticationFilter.setSessionAuthenticationStrategy(sessionStrategy);
		authenticationFilter.setRememberMeServices(rememberMeServices);
		authenticationFilter.setAuthenticationSuccessHandler(successHandler);
		authenticationFilter.setAuthenticationFailureHandler(failureHandler);
		authenticationFilter.setSessionAuthenticationStrategy(sessionStrategy);
		wechatAuthenticationFilter.setRememberMeServices(rememberMeServices);
		wechatAuthenticationFilter.setAuthenticationSuccessHandler(successHandler);
		wechatAuthenticationFilter.setAuthenticationFailureHandler(failureHandler);
		wechatAuthenticationFilter.setSessionAuthenticationStrategy(sessionStrategy);
		http.addFilterBefore(sessionFilter, FilterSecurityInterceptor.class);
		http.addFilterBefore(verifyCodeAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilterBefore(wechatAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilterAt(authenticationFilter, UsernamePasswordAuthenticationFilter.class);
	}

	protected UserDetailsService getUserDetailsService() {
		return clientService;
	}
}
