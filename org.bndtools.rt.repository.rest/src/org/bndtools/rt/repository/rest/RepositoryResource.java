package org.bndtools.rt.repository.rest;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.bndtools.rt.repository.api.QueryCache;
import org.bndtools.rt.repository.marshall.CapReqJson;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

@Path("/query")
public class RepositoryResource {
	
	private final JsonFactory jsonFactory = new JsonFactory();
	
	@Inject
	QueryCache queryCache;
	
	@Inject
	Repository repository;
	
	@Context
	private UriInfo uriInfo;
	
	@POST
	public Response createQuery(InputStream stream) throws Exception {
		List<Requirement> requirements = CapReqJson.parseRequirements(jsonFactory, stream);
		UUID queryId = queryCache.createQuery(requirements);
		
		URI createdUri = uriInfo.getAbsolutePathBuilder().path(queryId.toString()).build();
		return Response.created(createdUri).build();
	}
	
	@GET
	@Path("/{queryId}")
	public String getQuery(@PathParam("queryId") String queryId) throws Exception {
		UUID uuid = UUID.fromString(queryId);
		Collection<? extends Requirement> query = queryCache.getQuery(uuid);
		
		StringWriter writer = new StringWriter();
		JsonGenerator generator = jsonFactory.createJsonGenerator(writer);
		CapReqJson.writeRequirementArray(query, generator);
		generator.close();
		
		return writer.toString();
	}
	
}
