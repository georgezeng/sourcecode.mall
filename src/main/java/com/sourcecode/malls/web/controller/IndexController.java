package com.sourcecode.malls.web.controller;

import java.net.URL;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sourcecode.malls.context.ClientContext;

@RestController
public class IndexController {

	@RequestMapping(path = "/index")
	public String index(HttpServletRequest request, @RequestParam("url") String url) throws Exception {
		url = URLDecoder.decode(url, "UTF-8");
		if (ClientContext.get() != null && ClientContext.get().getId() != null) {
			URL uri = new URL(url);
			String query = uri.getQuery();
			if (StringUtils.isEmpty(query)) {
				query = "?";
			} else {
				query += "&";
			}
			String params = query + "uid=" + ClientContext.get().getId();
			url = url.replace("#", params + "#");
		}
		return String.format("<script>window.location.href='%s'</script>", url);
	}

}
