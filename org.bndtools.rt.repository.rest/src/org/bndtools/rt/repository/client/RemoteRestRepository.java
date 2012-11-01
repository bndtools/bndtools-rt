package org.bndtools.rt.repository.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import aQute.bnd.deployer.http.DefaultURLConnector;
import aQute.bnd.deployer.repository.CachingUriResourceHandle;
import aQute.bnd.deployer.repository.CachingUriResourceHandle.CachingMode;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.url.URLConnector;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.service.reporter.Reporter;

public class RemoteRestRepository implements Plugin, RepositoryPlugin, RegistryPlugin {
	
	public static String PROP_URL = "url";
	public static String PROP_NAME = "name";
	public static final String PROP_CACHE = "cache";
	
	private static final String DEFAULT_CACHE_DIR = ".bnd" + File.separator + "cache";

	private Reporter reporter;
	private File cacheDir = new File(System.getProperty("user.home") + File.separator + DEFAULT_CACHE_DIR);
	private URI baseUri;
	private String name;
	
	private Registry registry;
	
	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	public void setProperties(Map<String, String> configProps) {
		String baseUrlStr = configProps.get(PROP_URL);
		if (baseUrlStr == null)
			throw new IllegalArgumentException(String.format("Attribute '%s' must be set on plugin %s.", PROP_URL, getClass()));
		try {
			baseUri = new URI(baseUrlStr);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(String.format("Attribute '%s' is invalid on plugin %s.", PROP_URL, getClass()), e);
		}
		
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
			Iterable<JSONObject> iterable = parseJSONObjectList(new InputStreamReader(stream));
			for (JSONObject node : iterable) {
				Object bsnNode = node.get("bsn");
				if (bsnNode == null || !(bsnNode instanceof String))
					throw new Exception("Missing or invalid 'bsn' field.");
				result.add((String) bsnNode);
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
			Iterable<JSONObject> iterable = parseJSONObjectList(new InputStreamReader(stream));
			for (JSONObject node : iterable) {
				Object versionNode = node.get("version");
				if (versionNode == null || !(versionNode instanceof String))
					throw new Exception("Missing or invalid 'version' field.");
				Version version = new Version((String) versionNode);
				result.add(version);
			}
		} finally {
			IO.close(stream);
		}
		
		return result;
	}
	
	private static Iterable<JSONObject> parseJSONObjectList(Reader reader) throws Exception {
		Object root = new JSONParser().parse(reader);
		if (root instanceof JSONArray) {
			@SuppressWarnings("unchecked")
			Iterable<JSONObject> result = (Iterable<JSONObject>) root;
			return result;
		} else if (root instanceof JSONObject) {
			return Collections.singletonList((JSONObject) root);
		} else {
			throw new Exception("Root JSON node is neither an Array nor an Object.");
		}
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
