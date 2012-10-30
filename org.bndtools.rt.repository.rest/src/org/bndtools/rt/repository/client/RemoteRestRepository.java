package org.bndtools.rt.repository.client;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.ws.rs.core.UriBuilder;

import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

public class RemoteRestRepository implements RepositoryPlugin {
	
	public static String PROP_URL = "url";
	
	private final JsonFactory jsonFactory = new JsonFactory();
	private URI baseUri;

	public void setProperties(Map<String, String> configProps) throws Exception {
		String baseUrlStr = configProps.get(PROP_URL);
		if (baseUrlStr == null)
			throw new IllegalArgumentException(String.format("Attribute '%s' must be set on plugin %s.", PROP_URL, getClass()));
	
		baseUri = new URI(baseUrlStr);
	}


	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		return null;
	}

	@Override
	public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean canWrite() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		LinkedList<String> result = new LinkedList<String>();
		
		URI requestUri = UriBuilder.fromUri(baseUri).replaceQuery(pattern).build();
		WebResource resource = Client.create().resource(requestUri);
		String response = resource.get(String.class);
		
		JsonNode rootNode = new ObjectMapper(jsonFactory).readTree(response);
		Iterable<JsonNode> iterable = rootNode.isArray() ? rootNode : Collections.singletonList(rootNode);
		for (JsonNode node : iterable) {
			String bsn = node.get("bsn").asText();
			result.add(bsn);
		}
		
		return result;
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		SortedSet<Version> result = new TreeSet<Version>();
		
		URI requestUri = UriBuilder.fromUri(baseUri).path(bsn).build();
		WebResource resource = Client.create().resource(requestUri);
		String response = resource.get(String.class);
		
		JsonNode rootNode = new ObjectMapper(jsonFactory).readTree(response);
		Iterable<JsonNode> iterable = rootNode.isArray() ? rootNode : Collections.singletonList(rootNode);
		for (JsonNode node : iterable) {
			String versionStr = node.get("version").asText();
			Version version = new Version(versionStr);
			result.add(version);
		}		
		
		return result;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLocation() {
		// TODO Auto-generated method stub
		return null;
	}

}
