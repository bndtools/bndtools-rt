package org.example.tests.examples;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/foo6")
public class ClassResource6 {

	@Inject
	private List<Runnable> runnables;

	@GET
	@Produces("text/plain")
	public String getPlain() {
		String response = (runnables == null) ? "NULL" : Integer.toString(runnables.size());
		return response;
		
	}

}