package org.bndtools.rt.repository.server;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.bndtools.rt.repository.api.QueryCache;
import org.bndtools.rt.repository.marshall.CapReqJson;
import org.bndtools.rt.repository.marshall.Link;
import org.bndtools.rt.repository.marshall.LinkGenerator;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

@Path("/query")
public class QueryResource {
	
	private final JsonFactory jsonFactory = new JsonFactory();
	
	@Inject
	QueryCache queryCache;
	
	@Inject
	Repository repository;
	
	@Context
	private UriInfo uriInfo;
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createQuery(InputStream stream) throws Exception {
		List<Requirement> requirements = CapReqJson.parseRequirements(jsonFactory, stream);
		UUID queryId = queryCache.createQuery(requirements);
		
		URI createdUri = uriInfo.getAbsolutePathBuilder().path(queryId.toString()).build();
		return Response.created(createdUri).build();
	}
	
	@GET
	@Path("/{queryId}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getQuery(@PathParam("queryId") String queryId) throws Exception {
		UUID uuid = UUID.fromString(queryId);
		Collection<? extends Requirement> query = queryCache.getQuery(uuid);
		
		StringWriter writer = new StringWriter();
		JsonGenerator generator = jsonFactory.createJsonGenerator(writer);
		
		generator.writeStartObject();
		generator.writeStringField("id", queryId);
		
		URI providersUri = uriInfo.getAbsolutePathBuilder().path("providers").build();
		new LinkGenerator(generator).writeLinkArrayField(Collections.singletonList(new Link("providers", providersUri)));
		
		generator.writeFieldName("reqs");
		CapReqJson.writeRequirementArray(query, generator);
		generator.writeEndObject();
		
		generator.close();
		
		return writer.toString();
	}
	
	@GET
	@Path("/{queryId}/providers")
	@Produces(MediaType.APPLICATION_JSON)
	public String getProviders(@PathParam("queryId") String queryId) throws Exception {
		Map<Requirement, Collection<Capability>> providers = queryCache.getSolution(UUID.fromString(queryId), repository);
		
		StringWriter writer = new StringWriter();
		JsonGenerator generator = jsonFactory.createJsonGenerator(writer);
		
		CapReqJson.writeProviderArray(providers, generator);
		generator.close();
		
		return writer.toString();
	}
	
}
