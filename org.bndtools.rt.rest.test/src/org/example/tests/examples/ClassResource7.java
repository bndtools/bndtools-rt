package org.example.tests.examples;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.bndtools.inject.Optional;

@Path("/foo7")
public class ClassResource7 {

	@Inject
	@Optional
	private List<Runnable> runnables;

	@GET
	@Produces("text/plain")
	public String getPlain() {
		String response = (runnables == null) ? "NULL" : Integer.toString(runnables.size());
		return response;
		
	}

}