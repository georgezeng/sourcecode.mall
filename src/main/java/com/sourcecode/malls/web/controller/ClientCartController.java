package com.sourcecode.malls.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.dto.base.KeyDTO;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.client.ClientCartItemDTO;
import com.sourcecode.malls.repository.jpa.impl.client.ClientCartRepository;
import com.sourcecode.malls.service.impl.ClientService;

@RestController
@RequestMapping(path = "/client/cart")
public class ClientCartController {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ClientService clientService;

	@Autowired
	private ClientCartRepository cartRepository;

	@RequestMapping(path = "/total")
	public ResultBean<Integer> total() {
		return new ResultBean<>(cartRepository.findByClient(ClientContext.get()).size());
	}

	@RequestMapping(path = "/list")
	public ResultBean<ClientCartItemDTO> list(@RequestBody KeyDTO<Long> dto) {
		return new ResultBean<>(clientService.getCart(ClientContext.get()));
	}

	@RequestMapping(path = "/save")
	public ResultBean<Integer> save(@RequestBody ClientCartItemDTO dto) {
		return new ResultBean<>(clientService.saveCart(ClientContext.get(), dto));
	}
}
