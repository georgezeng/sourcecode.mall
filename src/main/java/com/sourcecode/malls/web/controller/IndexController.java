package com.sourcecode.malls.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sourcecode.malls.context.ClientContext;

@RestController
public class IndexController {

	@RequestMapping(path = "/index")
	public String index(HttpServletRequest request, @RequestParam("url") String url) {
		if (ClientContext.get() != null && ClientContext.get().getId() != null) {
			url = url.replace("#", "?uid=" + ClientContext.get().getId() + "#");
		}
		return String.format("<script>window.location.href='%s'</script>", url);
	}

}
