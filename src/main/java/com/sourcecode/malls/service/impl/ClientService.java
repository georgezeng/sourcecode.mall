package com.sourcecode.malls.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.merchant.Merchant;
import com.sourcecode.malls.properties.SuperAdminProperties;
import com.sourcecode.malls.repository.jpa.impl.client.ClientRepository;
import com.sourcecode.malls.repository.jpa.impl.merchant.MerchantRepository;
import com.sourcecode.malls.service.base.JpaService;
import com.sourcecode.malls.util.AssertUtil;

@Service("ClientDetailsService")
@Transactional
public class ClientService implements UserDetailsService, JpaService<Client, Long> {
	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private MerchantRepository merchantRepository;

	@Autowired
	private SuperAdminProperties adminProperties;

	@Transactional(readOnly = true)
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//		if (ClientContext.get() != null && adminProperties.getUsername().equals(username)) {
//			return ClientContext.get();
//		}
		if(adminProperties.getUsername().equals(username)) {
			Client admin = new Client();
			admin.setUsername(username);
			admin.setPassword("tt");
			admin.setEnabled(true);
			return admin;
		}
		Long merchantId = ClientContext.getMerchantId();
		if (merchantId == null) {
			throw new UsernameNotFoundException("用户名或密码有误");
		}
		Optional<Merchant> merchant = merchantRepository.findById(merchantId);
		if (!merchant.isPresent()) {
			throw new UsernameNotFoundException("用户名或密码有误");
		}
		Optional<Client> client = clientRepository.findByMerchantAndUsername(merchant.get(), username);
		if (!client.isPresent()) {
			throw new UsernameNotFoundException("用户名或密码有误");
		}
		return client.get();
	}

	public Client findByMerchantAndUsername(Long merchantId, String username) {
		Optional<Merchant> merchant = merchantRepository.findById(merchantId);
		AssertUtil.assertTrue(merchant.isPresent(), "商户不存在");
		Optional<Client> client = clientRepository.findByMerchantAndUsername(merchant.get(), username);
		AssertUtil.assertTrue(client.isPresent(), "用户不存在");
		return client.get();
	}

	@Override
	public JpaRepository<Client, Long> getRepository() {
		return clientRepository;
	}

}
