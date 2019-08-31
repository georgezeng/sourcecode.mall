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
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sourcecode.malls.constants.ExceptionMessageConstant;
import com.sourcecode.malls.constants.RequestParams;
import com.sourcecode.malls.constants.SystemConstant;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.client.ClientLevelSetting;
import com.sourcecode.malls.domain.client.ClientPoints;
import com.sourcecode.malls.domain.merchant.Merchant;
import com.sourcecode.malls.domain.merchant.MerchantShopApplication;
import com.sourcecode.malls.domain.redis.CodeStore;
import com.sourcecode.malls.repository.jpa.impl.client.ClientLevelSettingRepository;
import com.sourcecode.malls.repository.jpa.impl.client.ClientRepository;
import com.sourcecode.malls.repository.jpa.impl.coupon.ClientPointsRepository;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantShopApplicationRepository;
import com.sourcecode.malls.repository.redis.impl.CodeStoreRepository;
import com.sourcecode.malls.service.impl.ClientBonusService;
import com.sourcecode.malls.util.AssertUtil;

@Component
public class ClientVerifyCodeAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

	@Autowired
	private CodeStoreRepository codeStoreRepository;

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private ClientPointsRepository clientPointsRepository;

	@Autowired
	private ClientBonusService bonusService;

	@Autowired
	private MerchantShopApplicationRepository applicationRepository;

	@Autowired
	private ClientLevelSettingRepository levelRepository;

	public ClientVerifyCodeAuthenticationFilter() {
		super(new AntPathRequestMatcher("/login", "POST"));
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
		Optional<MerchantShopApplication> apOp = applicationRepository.findByDomain(domain);
		if (!apOp.isPresent()) {
			throw new AuthenticationServiceException("商户不存在");
		}
		Merchant merchant = apOp.get().getMerchant();
		String username = request.getParameter(RequestParams.USERNAME);
		String verifyCode = request.getParameter(RequestParams.PASSWORD);
		if (StringUtils.isEmpty(username) || StringUtils.isEmpty(verifyCode)) {
			throw new AuthenticationServiceException("手机号或验证码有误");
		}
		Optional<CodeStore> codeStoreOp = codeStoreRepository
				.findByCategoryAndKey(SystemConstant.LOGIN_VERIFY_CODE_CATEGORY, username + "_" + merchant.getId());
		AssertUtil.assertTrue(codeStoreOp.isPresent(), ExceptionMessageConstant.VERIFY_CODE_INVALID);
		AssertUtil.assertTrue(codeStoreOp.get().getValue().equals(verifyCode),
				ExceptionMessageConstant.VERIFY_CODE_INVALID);
		Optional<Client> userOp = clientRepository.findByMerchantAndUsername(merchant, username);
		Client user = null;
		if (!userOp.isPresent()) {
			user = new Client();
			user.setUsername(username);
			user.setMerchant(merchant);
			user.setEnabled(true);
			Client parent = null;
			String pidStr = request.getParameter("pid");
			if (!StringUtils.isEmpty(pidStr)) {
				Long pid = Long.valueOf(pidStr);
				Optional<Client> parentOp = clientRepository.findById(pid);
				if (parentOp.isPresent()) {
					parent = parentOp.get();
					user.setParent(parent);
				}
			}
			Optional<ClientLevelSetting> setting = levelRepository.findByMerchantAndLevel(merchant, 0);
			AssertUtil.assertTrue(setting.isPresent(), "商家尚未配置会员等级");
			user.setLevel(setting.get());
			clientRepository.save(user);
			ClientPoints points = new ClientPoints();
			points.setClient(user);
			clientPointsRepository.save(points);
			if (parent != null) {
				bonusService.addInviteBonus(user, parent);
			}
			bonusService.addRegistrationBonus(user.getId());
		} else {
			user = userOp.get();
			if (!user.isEnabled()) {
				throw new UsernameNotFoundException("账号已被禁用");
			}
		}
		codeStoreRepository.delete(codeStoreOp.get());
		return new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
	}

	@Override
	protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
		boolean matched = super.requiresAuthentication(request, response);
		return matched && "PhoneVerify".equals(request.getParameter(RequestParams.LOGIN_TYPE));
	}

	@Override
	public void afterPropertiesSet() {
	}

}
