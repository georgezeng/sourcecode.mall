package com.sourcecode.malls.web.controller;

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
import com.sourcecode.malls.domain.client.InvoiceTemplate;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.dto.order.InvoiceDTO;
import com.sourcecode.malls.dto.query.PageInfo;
import com.sourcecode.malls.repository.jpa.impl.merchant.InvoiceSettingRepository;
import com.sourcecode.malls.repository.jpa.impl.order.InvoiceTemplateRepository;
import com.sourcecode.malls.util.AssertUtil;

@RestController
@RequestMapping(path = "/client/invoice")
public class ClientInvoiceController {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private InvoiceTemplateRepository templateRepository;

	@Autowired
	private InvoiceSettingRepository settingRepository;

	@RequestMapping(path = "/save")
	public ResultBean<Void> save(@RequestBody InvoiceDTO dto) {
		Optional<InvoiceTemplate> op = Optional.of(new InvoiceTemplate());
		if (dto.getId() != null) {
			op = templateRepository.findById(dto.getId());
		} else {
			op.get().setClient(ClientContext.get());
		}
		BeanUtils.copyProperties(dto, op.get(), "id", "client");
		AssertUtil.assertTrue(op.isPresent() && op.get().getClient().getId().equals(ClientContext.get().getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		templateRepository.save(op.get());
		return new ResultBean<>();
	}

	@RequestMapping(path = "/list")
	public ResultBean<InvoiceDTO> list(@RequestBody PageInfo pageInfo) {
		return new ResultBean<>(templateRepository.findAllByClient(ClientContext.get(), pageInfo.pageable()).stream()
				.map(invoice -> invoice.asDTO()).collect(Collectors.toList()));
	}

	@RequestMapping(path = "/load/params/{id}")
	public ResultBean<InvoiceDTO> load(@PathVariable Long id) {
		Optional<InvoiceTemplate> op = templateRepository.findById(id);
		AssertUtil.assertTrue(op.isPresent() && op.get().getClient().getId().equals(ClientContext.get().getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		return new ResultBean<>(op.get().asDTO());
	}

	@RequestMapping(path = "/delete/params/{id}")
	public ResultBean<Void> delete(@PathVariable Long id) {
		Optional<InvoiceTemplate> op = templateRepository.findById(id);
		AssertUtil.assertTrue(op.isPresent() && op.get().getClient().getId().equals(ClientContext.get().getId()),
				ExceptionMessageConstant.NO_SUCH_RECORD);
		templateRepository.delete(op.get());
		return new ResultBean<>();
	}

	@RequestMapping(path = "/content/list")
	public ResultBean<String> contentList() {
		PageInfo info = new PageInfo();
		info.setNum(1);
		info.setSize(99999999);
		return new ResultBean<>(settingRepository.findAllByMerchant(ClientContext.get().getMerchant(), info.pageable())
				.stream().map(invoice -> invoice.getContent()).collect(Collectors.toList()));
	}

}
