package com.sourcecode.malls.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sourcecode.malls.constants.ExceptionMessageConstant;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.client.ClientCartItem;
import com.sourcecode.malls.domain.goods.GoodsItem;
import com.sourcecode.malls.domain.goods.GoodsItemProperty;
import com.sourcecode.malls.dto.client.ClientCartItemDTO;
import com.sourcecode.malls.dto.goods.GoodsAttributeDTO;
import com.sourcecode.malls.repository.jpa.impl.client.ClientCartRepository;
import com.sourcecode.malls.service.base.JpaService;
import com.sourcecode.malls.util.AssertUtil;

@Service
@Transactional
public class ClientCartService implements JpaService<ClientCartItem, Long> {

	@Autowired
	private GoodsItemService itemService;

	@Autowired
	private ClientCartRepository cartRepository;

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
//		AssertUtil.assertTrue(!cartItem.isPresent(), "已经添加过该商品");
		ClientCartItem data = cartItem.orElseGet(ClientCartItem::new);
		data.setNums(dto.getNums() + data.getNums());
		data.setItem(item.get());
		data.setProperty(property);
		data.setClient(client);
		cartRepository.save(data);
	}

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
			List<String> attrs = new ArrayList<>();
			for (GoodsAttributeDTO value : dto.getProperty().getValues()) {
				attrs.add(value.getName());
			}
			dto.setAttrs(attrs);
			list.add(dto);
		}
		return list;
	}

	@Override
	public JpaRepository<ClientCartItem, Long> getRepository() {
		return cartRepository;
	}

}
