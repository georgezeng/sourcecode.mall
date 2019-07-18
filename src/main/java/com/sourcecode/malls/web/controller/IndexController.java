package com.sourcecode.malls.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {

	@RequestMapping(path = "/index")
	public String index(HttpServletRequest request, @RequestParam("url") String url) {
		return String.format("<script>window.location.href='%s'</script>", url);
	}

}
