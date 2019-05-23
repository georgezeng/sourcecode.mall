package com.sourcecode.malls.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {

	@RequestMapping(path = "/index")
	public String index(HttpServletRequest request) {
		return String.format("<script>window.location.href='%s'</script>", "https://www.dobaishop.com/#/Login");
	}

}
