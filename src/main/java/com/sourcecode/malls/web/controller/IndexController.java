package com.sourcecode.malls.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sourcecode.malls.constants.EnvConstant;
import com.sourcecode.malls.context.ClientContext;

@RestController
public class IndexController {

	@Autowired
	private Environment env;

	@RequestMapping(path = "/index")
	public String index(HttpServletRequest request, @RequestParam("url") String url) {
		if (ClientContext.get() != null && ClientContext.get().getId() != null) {
			String params = "?uid=" + ClientContext.get().getId();
			if (env.acceptsProfiles(Profiles.of(EnvConstant.UAT))) {
				params += "&eruda=true";
			}
			url = url.replace("#", params + "#");
		} else {
			String params = "?";
			if (env.acceptsProfiles(Profiles.of(EnvConstant.UAT))) {
				params += "eruda=true";
			}
			url = url.replace("#", params + "#");
		}
		return String.format("<script>window.location.href='%s'</script>", url);
	}

}
