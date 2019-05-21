package com.sourcecode.malls.web.security.rememberme;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.stereotype.Component;

import com.aliyuncs.utils.StringUtils;
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

	@Override
	protected UserDetails processAutoLoginCookie(String[] cookieTokens, HttpServletRequest request, HttpServletResponse response) {
		try {
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
			return super.processAutoLoginCookie(cookieTokens, request, response);
		} finally {
			ClientContext.setMerchantId(null);
		}
	}

}
