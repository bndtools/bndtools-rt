package org.example.tests.examples;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/foo2")
public class ClassResource2 {

	@Inject
	private Runnable runnable;

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