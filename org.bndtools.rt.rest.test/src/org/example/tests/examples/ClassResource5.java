package org.example.tests.examples;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.bndtools.inject.Optional;
import org.bndtools.inject.TargetFilter;

@Path("/foo5")
public class ClassResource5 {

	@Optional
	@Inject
	@TargetFilter("(foo=bar)")
	private Runnable runnable;

	@GET
	@Produces("text/plain")
	public String getPlain() {
		String response = (runnable == null) ? "NULL" : "NOT NULL";
		return response;
		
	}

}