package com.sourcecode.malls.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sourcecode.malls.constants.ExceptionMessageConstant;
import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.client.ClientCartItem;
import com.sourcecode.malls.domain.goods.GoodsItem;
import com.sourcecode.malls.domain.goods.GoodsItemProperty;
import com.sourcecode.malls.domain.goods.GoodsItemValue;
import com.sourcecode.malls.domain.merchant.Merchant;
import com.sourcecode.malls.dto.client.ClientCartItemDTO;
import com.sourcecode.malls.enums.Sex;
import com.sourcecode.malls.properties.SuperAdminProperties;
import com.sourcecode.malls.repository.jpa.impl.client.ClientCartRepository;
import com.sourcecode.malls.repository.jpa.impl.client.ClientRepository;
import com.sourcecode.malls.repository.jpa.impl.goods.GoodsItemValueRepository;
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

	@Autowired
	private GoodsItemService itemService;

	@Autowired
	private ClientCartRepository cartRepository;

	@Autowired
	protected GoodsItemValueRepository valueRepository;

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

	public void saveCart(Client client, ClientCartItemDTO dto) {
		Optional<GoodsItem> item = itemService.findById(dto.getItemId());
		AssertUtil.assertTrue(item.isPresent() && item.get().getMerchant().getId().equals(client.getMerchant().getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		GoodsItemProperty property = null;
		for (GoodsItemProperty p : item.get().getProperties()) {
			if (p.getId().equals(dto.getPropertyId())) {
				property = p;
				break;
			}
		}
		AssertUtil.assertNotNull(property, "商品没有规格");
		Optional<ClientCartItem> cartItem = cartRepository.findByClientAndProperty(client, property);
		AssertUtil.assertTrue(!cartItem.isPresent(), "已经添加过该商品");
		ClientCartItem data = new ClientCartItem();
		data.setNums(dto.getNums());
		data.setItem(item.get());
		data.setProperty(property);
		data.setClient(client);
		cartRepository.save(data);
	}

	@Transactional(readOnly = true)
	public List<ClientCartItemDTO> getCart(Client client) {
		List<ClientCartItem> cart = cartRepository.findByClient(client);
		List<ClientCartItemDTO> list = new ArrayList<>();
		for (ClientCartItem cartItem : cart) {
			Calendar c = Calendar.getInstance();
			c.setTime(cartItem.getUpdateTime());
			c.add(Calendar.DATE, 7);
			if (new Date().compareTo(c.getTime()) >= 0) {
				cartRepository.delete(cartItem);
				continue;
			}
			ClientCartItemDTO dto = cartItem.asDTO();
			List<GoodsItemValue> values = valueRepository.findAllByUid(dto.getProperty().getUid());
			List<String> attrs = new ArrayList<>();
			for (GoodsItemValue value : values) {
				attrs.add(value.getValue().getName());
			}
			dto.setAttrs(attrs);
			list.add(dto);
		}
		return list;
	}

	@Override
	public JpaRepository<Client, Long> getRepository() {
		return clientRepository;
	}

}
