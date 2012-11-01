package org.bndtools.rt.repository.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import aQute.bnd.deployer.http.DefaultURLConnector;
import aQute.bnd.deployer.repository.CachingUriResourceHandle;
import aQute.bnd.deployer.repository.CachingUriResourceHandle.CachingMode;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.url.URLConnector;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RemoteRestRepository implements RepositoryPlugin, RegistryPlugin {
	
	public static String PROP_URL = "url";
	public static String PROP_NAME = "name";
	public static final String PROP_CACHE = "cache";
	
	private static final String DEFAULT_CACHE_DIR = ".bnd" + File.separator + "cache";

	private final JsonFactory jsonFactory = new JsonFactory();
	
	private File cacheDir = new File(System.getProperty("user.home") + File.separator + DEFAULT_CACHE_DIR);
	private URI baseUri;
	private String name;
	
	private Registry registry;

	public void setProperties(Map<String, String> configProps) throws Exception {
		String baseUrlStr = configProps.get(PROP_URL);
		if (baseUrlStr == null)
			throw new IllegalArgumentException(String.format("Attribute '%s' must be set on plugin %s.", PROP_URL, getClass()));
		baseUri = new URI(baseUrlStr);
		
		name = configProps.get(PROP_NAME);

		String cachePath = configProps.get(PROP_CACHE);
		if (cachePath != null) {
			cacheDir = new File(cachePath);
			if (!cacheDir.isDirectory())
				try {
					throw new IllegalArgumentException(String.format(
							"Cache path '%s' does not exist, or is not a directory.", cacheDir.getCanonicalPath()));
				}
				catch (IOException e) {
					throw new IllegalArgumentException("Could not get cacheDir canonical path", e);
				}
		}
}
	
	@Override
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	@Override
	public PutResult put(InputStream inputStream, PutOptions options) throws Exception {
		URI postUri = new URI(baseUri.getScheme(), baseUri.getUserInfo(), baseUri.getHost(), baseUri.getPort(), baseUri.getPath() + "/bundles", null, null);
		
		HttpURLConnection conn = (HttpURLConnection) postUri.toURL().openConnection();
		conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM);
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		
		// Send the data
		byte[] buffer = new byte[1024];
		OutputStream postStream = conn.getOutputStream();
		try {
			while (true) {
				int bytesRead = inputStream.read(buffer, 0, 1024);
				if (bytesRead < 0) break;
				postStream.write(buffer, 0, bytesRead);
			}
			postStream.flush();
		} finally {
			IO.close(postStream);
		}
		
		// Read the response header
		int response = conn.getResponseCode();
		if (response < 200 || response >= 300)
			throw new Exception(String.format("Server returned error code %d", response));
		String location = conn.getHeaderField("Location");
		if (location == null)
			throw new Exception("Server did not return a Location header");
		
		PutResult result = new PutResult();
		result.artifact = new URI(location);
		result.digest = null;
		
		return result;
	}

	@Override
	public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners) throws Exception {
		URLConnector connector = registry != null ? registry.getPlugin(URLConnector.class) : null;
		if (connector == null) connector = new DefaultURLConnector();
		
		URI bundleUri = new URI(baseUri.getScheme(), baseUri.getUserInfo(), baseUri.getHost(), baseUri.getPort(), baseUri.getPath() + "/bundles/" + bsn + "/" + version, null, null);
		CachingUriResourceHandle handle = new CachingUriResourceHandle(bundleUri, cacheDir, connector, CachingMode.PreferRemote);
		return handle.request();
	}

	@Override
	public boolean canWrite() {
		return true;
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		LinkedList<String> result = new LinkedList<String>();
		
		URI requestUri = new URI(baseUri.getScheme(), baseUri.getUserInfo(), baseUri.getHost(), baseUri.getPort(), baseUri.getPath() + "/bundles", "pattern=" + pattern, null);
		InputStream stream = requestUri.toURL().openStream();
		
		try {
			JsonNode rootNode = new ObjectMapper(jsonFactory).readTree(stream);
			Iterable<JsonNode> iterable = rootNode.isArray() ? rootNode : Collections.singletonList(rootNode);
			for (JsonNode node : iterable) {
				String bsn = node.get("bsn").asText();
				result.add(bsn);
			}
		} finally {
			IO.close(stream);
		}
		
		return result;
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		SortedSet<Version> result = new TreeSet<Version>();
		
		URI requestUri = new URI(baseUri.getScheme(), baseUri.getUserInfo(), baseUri.getHost(), baseUri.getPort(), baseUri.getPath() + "/bundles/" + bsn, null, null);
		InputStream stream = requestUri.toURL().openStream();
		
		try {
			JsonNode rootNode = new ObjectMapper(jsonFactory).readTree(stream);
			Iterable<JsonNode> iterable = rootNode.isArray() ? rootNode : Collections.singletonList(rootNode);
			for (JsonNode node : iterable) {
				String versionStr = node.get("version").asText();
				Version version = new Version(versionStr);
				result.add(version);
			}
		} finally {
			IO.close(stream);
		}
		
		return result;
	}

	@Override
	public String getName() {
		return name != null ? name : baseUri.toString();
	}

	@Override
	public String getLocation() {
		return baseUri.toString();
	}

}
