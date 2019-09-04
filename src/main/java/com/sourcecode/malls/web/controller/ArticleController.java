package com.sourcecode.malls.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sourcecode.malls.context.ClientContext;
import com.sourcecode.malls.dto.article.ArticleDTO;
import com.sourcecode.malls.dto.base.ResultBean;
import com.sourcecode.malls.service.impl.ArticleService;

@RestController
@RequestMapping(path = "/article")
public class ArticleController {

	@Autowired
	private ArticleService service;

	@RequestMapping(path = "/search/{title}")
	public ResultBean<ArticleDTO> search(@PathVariable String title) {
		return new ResultBean<>(service.search(ClientContext.getMerchantId(), title));
	}

}
