package org.bndtools.rt.headlessJS.test.testPages;


import java.io.IOException;
import java.net.URI;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.bndtools.rt.headlessJS.test.testPages.MyFilter.Config;
import org.bndtools.service.headlessJS.HeadlessJS;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Reference;

@Component(provide = Filter.class, designateFactory = Config.class, configurationPolicy = ConfigurationPolicy.optional, properties = "pattern=/test")
public class MyFilter implements Filter {

	HeadlessJS headlessJS;

	@Reference
	public void setHeadlessJS(HeadlessJS headlessJS) {
		this.headlessJS = headlessJS;
	}

	interface Config {
		String pattern();

		String init_param();
	}

	@Override
	public void destroy() {
		System.out.println("Filter - destroy()");
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {

		
		// Finding _escaped_fragment_
		String prefix = "_escaped_fragment_=";
		String queryString = ((HttpServletRequest) req).getQueryString();
		if (queryString != null && queryString.startsWith(prefix)) {

			StringBuffer normalUrl = ((HttpServletRequest) req).getRequestURL();
			normalUrl.append("#!");
			normalUrl.append(queryString.substring(prefix.length()));

			System.out.println(queryString + " -> " + normalUrl);
			
			try {
				headlessJS.getInterpretedPage(new URI(normalUrl.toString()), resp.getOutputStream());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		} else {
			chain.doFilter(req, resp);
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		System.out.println("Filter - init()");
	}

}
