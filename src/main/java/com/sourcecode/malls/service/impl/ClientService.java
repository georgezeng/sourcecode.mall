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
import com.sourcecode.malls.enums.Sex;
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
		Long merchantId = ClientContext.getMerchantId();
		AssertUtil.assertNotNull(merchantId, "商户不存在");
		Optional<Merchant> merchant = merchantRepository.findById(merchantId);
		if (adminProperties.getUsername().equals(username)) {
			return getAdmin(merchant.get());
		}
		AssertUtil.assertTrue(merchant.isPresent(), "商户不存在");
		Optional<Client> client = clientRepository.findByMerchantAndUsername(merchant.get(), username);
		if (!client.isPresent()) {
			throw new UsernameNotFoundException("用户名或密码有误");
		}
		return client.get();
	}

	@Transactional(readOnly = true)
	public Client getAdmin(Merchant merchant) {
		Client admin = new Client();
		admin.setId(0l);
		admin.setUsername(adminProperties.getUsername());
		admin.setPassword(adminProperties.getPassword());
		admin.setNickname("管理员");
		admin.setSex(Sex.Secret);
		admin.setEnabled(true);
		admin.setMerchant(merchant);
		admin.setAuth(adminProperties.getAuthority());
		return admin;
	}

	@Transactional(readOnly = true)
	public Client findByMerchantAndUsername(Long merchantId, String username) {
		Optional<Merchant> merchant = merchantRepository.findById(merchantId);
		AssertUtil.assertTrue(merchant.isPresent(), "商户不存在");
		if (username.equals(adminProperties.getUsername())) {
			return getAdmin(merchant.get());
		}
		Optional<Client> client = clientRepository.findByMerchantAndUsername(merchant.get(), username);
		AssertUtil.assertTrue(client.isPresent(), "用户不存在");
		client.get().getMerchant();
		return client.get();
	}

	@Transactional(readOnly = true)
	public Optional<Client> findById(Long id) {
		Optional<Client> client = clientRepository.findById(id);
		AssertUtil.assertTrue(client.isPresent(), "用户不存在");
		client.get().getMerchant();
		return client;
	}

	@Override
	public JpaRepository<Client, Long> getRepository() {
		return clientRepository;
	}

}
