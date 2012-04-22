package org.example.tests.examples;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

@Component(provide = Object.class, properties = "osgi.rest.alias=/example2")
@Path("/foo")
public class SingletonServiceResource2 {

	private Runnable runnable;

	@Reference
	public void setRunnable(Runnable runnable) {
		this.runnable = runnable;
	}

	@GET
	@Produces("text/html")
	public String getHtml() {
		runnable.run();
		return "<html><head></head><body>\n"
				+ "This is an easy resource (as html text).\n"
				+ "</body></html>";
	}

	@GET
	@Produces("text/plain")
	public String getPlain() {
		runnable.run();
		return "This is an easy resource (as plain text)";
	}
}