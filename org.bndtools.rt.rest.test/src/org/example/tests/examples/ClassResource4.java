package org.example.tests.examples;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.bndtools.inject.Optional;

// Exactly the same as ClassResource3, but the @Optional and @Inject annotations are the other way around
@Path("/foo4")
public class ClassResource4 {

	@Inject
	@Optional
	private Runnable runnable;

	@GET
	@Produces("text/plain")
	public String getPlain() {
		String response = (runnable == null) ? "NULL" : "NOT NULL";
		return response;
		
	}

}