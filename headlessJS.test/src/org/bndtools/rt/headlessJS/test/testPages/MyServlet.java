package org.bndtools.rt.headlessJS.test.testPages;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bndtools.rt.headlessJS.test.testPages.MyServlet.Config;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Reference;

@Component(provide = {Servlet.class, HttpContext.class}, designateFactory = Config.class, configurationPolicy = ConfigurationPolicy.optional, properties = "alias=/test")
public class MyServlet extends HttpServlet implements HttpContext {
		
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	interface Config {
		String alias();
	}

	@Reference
	public void setHttpService(HttpService service) throws Exception {
		service.registerResources("/static", "/static", this);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		PrintWriter out = resp.getWriter();
		out.println("<html>");
		out.println("Normal page<br/>");
		out.println("Fragment: ");
		out.println("<script type='text/javascript' src='static/test.js'></script>");
		out.println("</html>");
	}
	@Override
	public boolean handleSecurity(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		return true;
	}
	@Override
	public URL getResource(String name) {
		URL u = getClass().getClassLoader().getResource(name);
		return u;
	}
	@Override
	public String getMimeType(String name) {
		return null;
	}

}