package com.sourcecode.malls.web.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sourcecode.malls.constants.ExceptionMessageConstant;
import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.domain.client.Client;
import com.sourcecode.malls.domain.client.ClientAddress;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.client.ClientAddressDTO;
import com.sourcecode.malls.dto.query.PageInfo;
import com.sourcecode.malls.repository.jpa.impl.client.ClientAddressRepository;
import com.sourcecode.malls.util.AssertUtil;

@RestController
@RequestMapping(path = "/client/address")
public class ClientAddressController {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ClientAddressRepository repository;

	@RequestMapping(path = "/save")
	public ResultBean<Void> save(@RequestBody ClientAddressDTO dto) {
		ClientAddress data = new ClientAddress();
		BeanUtils.copyProperties(dto, data, "id");
		if (data.getId() == null) {
			Client client = ClientContext.get();
			data.setClient(client);
		}
		repository.save(data);
		return new ResultBean<>();
	}

	@RequestMapping(path = "/list")
	public ResultBean<ClientAddressDTO> list(@RequestBody PageInfo pageInfo) {
		Client client = ClientContext.get();
		List<ClientAddress> list = repository.findByClient(client, pageInfo.pageable());
		if (list == null) {
			list = new ArrayList<>();
		}
		return new ResultBean<>(list.stream().map(address -> address.asDTO()).collect(Collectors.toList()));
	}

	@RequestMapping(path = "/load/params/{id}")
	public ResultBean<ClientAddressDTO> load(@PathVariable Long id) {
		ClientAddress data = repository.findById(id).orElseGet(ClientAddress::new);
		AssertUtil.assertTrue(data.getClient().getId().equals(ClientContext.get().getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		return new ResultBean<>(data.asDTO());
	}

	@RequestMapping(path = "/asDefault/params/{id}")
	public ResultBean<Void> asDefault(@PathVariable Long id) {
		Optional<ClientAddress> data = repository.findById(id);
		AssertUtil.assertTrue(data.isPresent() && data.get().getClient().getId().equals(ClientContext.get().getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		repository.clearDefaultStatus(ClientContext.get().getId());
		data.get().setDefault(true);
		repository.save(data.get());
		return new ResultBean<>();
	}

	@RequestMapping(path = "/delete/params/{id}")
	public ResultBean<Void> delete(@PathVariable Long id) {
		Optional<ClientAddress> data = repository.findById(id);
		AssertUtil.assertTrue(data.isPresent() && data.get().getClient().getId().equals(ClientContext.get().getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		repository.delete(data.get());
		return new ResultBean<>();
	}

}
