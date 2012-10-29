package org.bndtools.rt.repository.server;

import java.io.StringWriter;
import java.net.URI;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import aQute.bnd.service.IndexProvider;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

@Path("/index")
public class IndexResource {

	private final JsonFactory jsonFactory = new JsonFactory();
	
	@Inject
	IndexProvider indexProvider;
	
	@Context
	UriInfo uriInfo;
	
	private static enum RepoLinkType {
		hosted, ext
	}
	
	@GET
	public String listIndexes() throws Exception {
		List<URI> locations = indexProvider.getIndexLocations();
		
		StringWriter writer = new StringWriter();
		JsonGenerator gen = jsonFactory.createJsonGenerator(writer);
		
		UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder().path("{count}");
		
		gen.writeStartArray();
		int count = 0;
		for (URI location : locations) {
			gen.writeStartObject();
			
			RepoLinkType linkType;
			URI hrefUri;
			if ("file".equals(location.getScheme())) {
				linkType = RepoLinkType.hosted;
				hrefUri = uriBuilder.build(count);
			} else {
				linkType = RepoLinkType.ext;
				hrefUri = location;
			}

			gen.writeStringField("type", linkType.toString());
			gen.writeStringField("href", hrefUri.toString());
			gen.writeEndObject();
		}
		gen.writeEndArray();
		gen.close();
		
		return writer.toString();
	}
	
}
