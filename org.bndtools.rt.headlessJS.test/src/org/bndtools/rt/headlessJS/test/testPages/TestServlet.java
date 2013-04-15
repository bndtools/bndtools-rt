package org.bndtools.rt.headlessJS.test.testPages;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bndtools.rt.headlessJS.test.testPages.TestServlet.Config;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Reference;

@Component(provide = {Servlet.class, HttpContext.class}, 
			designateFactory = Config.class, 
			configurationPolicy = ConfigurationPolicy.optional, 
			properties = "alias=/test")
public class TestServlet extends HttpServlet implements HttpContext {
	
	public static String rawPage;
	public static String interpretedPage;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	interface Config {
		String alias();
	}

	@Activate
	public void activate() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>\n");
		sb.append("Normal page<br/>\n");
		sb.append("<hr />");
		sb.append("External script: <script type='text/javascript' src='static/test.js'></script><br />\n");
		sb.append("<hr />\n");
		sb.append("Internal script: <script type='text/javascript'>document.write('Hello, World!')</script><br />\n");
		sb.append("</html>");
		
		rawPage = sb.toString();
		
		sb = new StringBuilder();
		sb.append("<html>\n");
		sb.append("Normal page<br/>\n");
		sb.append("<hr />");
		sb.append("External script: <script type='text/javascript' src='static/test.js'></script>Hello, World!<br />\n");
		sb.append("<hr />\n");
		sb.append("Internal script: <script type='text/javascript'>document.write('Hello, World!')</script>Hello, World!<br />\n");
		sb.append("</html>");

	}
	
	@Reference
	public void setHttpService(HttpService service) throws Exception {
		service.registerResources("/static", "/static", this);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		PrintWriter out = resp.getWriter();
		out.print(rawPage);
		out.flush();
		out.close();
		
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