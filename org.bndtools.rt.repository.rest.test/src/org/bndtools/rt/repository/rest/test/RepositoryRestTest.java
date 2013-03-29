/*******************************************************************************
 * Copyright (c) 2012 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package org.bndtools.rt.repository.rest.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;

import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.w3c.dom.Document;

import aQute.lib.io.IO;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;

public class RepositoryRestTest extends TestCase {
	
	static {
		try {
			IO.deleteWithException(new File("generated/repodir1"));
			IO.deleteWithException(new File("generated/repodir2"));
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

    private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    
    /*
	public void testPostQueryAndReadBack() throws Exception {
    	// Setup the mock cache
    	MockQueryCache mockCache = new MockQueryCache();
    	UUID uuid = UUID.randomUUID();
    	mockCache.setUuid(uuid);
    	Collection<Requirement> mockResponse = Collections.singletonList(new CapReqBuilder("osgi.extender").addDirective("filter", "(osgi.extender=osgi.ds)").buildSyntheticRequirement());
    	mockCache.setRequirements(mockResponse);
    	
    	// Registry the mocks
    	ServiceRegistration cacheReg = context.registerService(QueryCache.class.getName(), mockCache, null);
    	ServiceRegistration repoReg = context.registerService(Repository.class.getName(), new MockRepository(), null);
    	
    	// POST the query
    	String queryText = IO.collect(RepositoryRestTest.class.getResourceAsStream("query1.json")).replaceAll("\\s", "");
    	ClientResource client = new ClientResource("http://127.0.0.1:8080/repo/query");
    	client.setRetryOnError(false);
    	client.post(new StringRepresentation(queryText, MediaType.APPLICATION_JSON));
    	Response response = client.getResponse();
    	Reference queryLocation = response.getLocationRef();
    	
    	// Check the POST result
    	assertEquals(Status.SUCCESS_CREATED.getCode(), response.getStatus().getCode());
		assertEquals("http://127.0.0.1:8080/repo/query/" + uuid, queryLocation.toString());
		
		// GET the query back
		client = new ClientResource(queryLocation);
		client.setRetryOnError(false);
		Representation rep = client.get(MediaType.APPLICATION_JSON);
		
		// Check the GET result
		StringWriter writer = new StringWriter();
		rep.write(writer);
		String actualOutput = writer.toString().trim();
		String expectedOutput = IO.collect(RepositoryRestTest.class.getResourceAsStream("query1-saved.json")).replaceAll("\\s", "").replace("%QUERY_ID%", uuid.toString());
		assertEquals(expectedOutput, actualOutput);
    	
		// Tidy up
    	cacheReg.unregister();
    	repoReg.unregister();
    }
    
    public void testPostQueryGetSolution() throws Exception {
    	// Setup the mock cache
    	MockQueryCache mockCache = new MockQueryCache();
    	UUID uuid = UUID.randomUUID();
    	mockCache.setUuid(uuid);
    	
    	// Setup the providers result for the mock repository
    	Requirement requirement = new CapReqBuilder("osgi.extender").addDirective("filter", "(osgi.extender=osgi.ds)").buildSyntheticRequirement();
    	CapReqBuilder capBuilder = new CapReqBuilder("osgi.extender").addAttribute("osgi.extender", "osgi.ds").addAttribute("version", new Version("1.2"));
    	Resource resource = new ResourceBuilder().addCapability(capBuilder).build();
    	Map<Requirement, Collection<Capability>> providers = new HashMap<Requirement, Collection<Capability>>();
    	providers.put(requirement, resource.getCapabilities("osgi.extender"));
    	
    	// Setup the mock repository
    	MockRepository mockRepo = new MockRepository();
    	mockRepo.setResult(providers);
    	
    	// Register the mocks
    	ServiceRegistration cacheReg = context.registerService(QueryCache.class.getName(), mockCache, null);
		ServiceRegistration repoReg = context.registerService(Repository.class.getName(), mockRepo, null);
    
		// POST the query
		String queryText = IO.collect(RepositoryRestTest.class.getResourceAsStream("query1.json"));
    	ClientResource client = new ClientResource("http://127.0.0.1:8080/repo/query");
    	client.setRetryOnError(false);
    	client.post(new StringRepresentation(queryText, MediaType.APPLICATION_JSON));
    	Response response = client.getResponse();
    	Reference queryLocation = response.getLocationRef();

    	// Check the POST result
    	assertEquals(Status.SUCCESS_CREATED.getCode(), response.getStatus().getCode());
		assertEquals("http://127.0.0.1:8080/repo/query/" + uuid, queryLocation.toString());
		
		// GET the result
		client = new ClientResource(queryLocation.addSegment("providers"));
		client.setRetryOnError(false);
		Representation providerRepresentation = client.get(MediaType.APPLICATION_JSON);
		
		// Check the result
		StringWriter providerOutput = new StringWriter();
		providerRepresentation.write(providerOutput);
		String expectedOutput = IO.collect(RepositoryRestTest.class.getResourceAsStream("result1.json")).replaceAll("\\s", "");
 		String actualOutput = providerOutput.toString().trim();
		assertEquals(expectedOutput.trim(), actualOutput);
		
		// Tidy up
		cacheReg.unregister();
		repoReg.unregister();
    }
    
    */
    
    public void testList() throws Exception {
    	WebResource resource = Client.create().resource("http://127.0.0.1:8080/testrepo/bundles?pattern=org.*");
    	String result = resource.get(String.class);
    	String expected = IO.collect(RepositoryRestTest.class.getResourceAsStream("bsns.json")).replaceAll("\\s", "").replace("%REPO_URI_PREFIX%", "http://127.0.0.1:8080/testrepo");
    	assertEquals(expected, result);
    }
    
    public void testVersions() throws Exception {
    	WebResource resource = Client.create().resource("http://127.0.0.1:8080/testrepo/bundles/org.example.foo");
    	String result = resource.get(String.class);
    	String expected = IO.collect(RepositoryRestTest.class.getResourceAsStream("versions.json")).replaceAll("\\s", "").replace("%REPO_URI_PREFIX%", "http://127.0.0.1:8080/testrepo");
    	assertEquals(expected, result);
    }
    
    public void testGetBundleContextByBsnAndVersion() {
    	Client client = Client.create();
    	client.setFollowRedirects(false);
		WebResource resource = client.resource("http://127.0.0.1:8080/testrepo/bundles/org.example.foo/1.2.3.qualifier");
    	ClientResponse response = resource.get(ClientResponse.class);
    	assertEquals(Status.SEE_OTHER.getStatusCode(), response.getStatus());
    	String location = response.getLocation().toString();
		assertEquals("http://127.0.0.1:8080/testrepo/org.example.foo/org.example.foo-1.2.3.jar", location);
    }
    
    public void testGetIndexContentsZippedToPlain() throws Exception {
		// GET the index content
    	Client c = Client.create();
    	WebResource resource = c.resource("http://127.0.0.1:8080/repo1/index");
    	String result = resource.accept(MediaType.APPLICATION_XML).get(String.class);
    	
		// Check the index XML
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new ByteArrayInputStream(result.getBytes("UTF-8")));
		assertEquals("repository", doc.getDocumentElement().getNodeName());
    }
    
    public void testGetIndexContentsUnzipped() throws Exception {
		// GET the index content
    	Client c = Client.create();
    	WebResource resource = c.resource("http://127.0.0.1:8080/repo2/index");
    	String result = resource.accept(MediaType.APPLICATION_XML).get(String.class);
    	
		// Check the index XML
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new ByteArrayInputStream(result.getBytes("UTF-8")));
		assertEquals("repository", doc.getDocumentElement().getNodeName());
    }
    
    public void testGetIndexContentsZipped() throws Exception {
    	/*
    	 * TODO
    	 * 
		// GET the index content
    	Client c = Client.create();
    	WebResource resource = c.resource("http://127.0.0.1:8080/repo1/index");
    	ClientResponse response = resource
    			.accept(MediaType.APPLICATION_XML)
    			.header("Accept-Encoding", "compress, gzip")
    			.get(ClientResponse.class);
    	
		// Check the index XML
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new GZIPInputStream(response.getEntityInputStream()));
		assertEquals("repository", doc.getDocumentElement().getNodeName());
    	 */
    }
    
    public void testPostNonBundle() throws Exception {
		// Generate a JAR in memory
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Foo", "foo");
		ByteArrayOutputStream bundleBuffer = new ByteArrayOutputStream();
		generateJar(bundleBuffer, headers, 10, 10);
		InputStream bundleInput = new ByteArrayInputStream(bundleBuffer.toByteArray());
		
		// POST the bundle
		WebResource resource = Client.create().resource("http://127.0.0.1:8080/repo1/bundles");
		ClientResponse response = resource.entity(bundleInput, MediaType.APPLICATION_OCTET_STREAM_TYPE)
			.post(ClientResponse.class);
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    public void testPostBundle() throws Exception {
		// Generate a JAR in memory
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		headers.put(Constants.BUNDLE_SYMBOLICNAME, "org.example.foo");
		headers.put(Constants.BUNDLE_VERSION, "1.2.3.qualifier");
		ByteArrayOutputStream bundleBuffer = new ByteArrayOutputStream();
		generateJar(bundleBuffer, headers, 10, 10);
		InputStream bundleInput = new ByteArrayInputStream(bundleBuffer.toByteArray());
		
		// POST the bundle
		WebResource resource = Client.create().resource("http://127.0.0.1:8080/repo1/bundles");
		ClientResponse response = resource.entity(bundleInput, new MediaType("application", "vnd.osgi.bundle"))
				.post(ClientResponse.class);
		URI location = response.getLocation();
    	
    	// Check the POST result
		assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    	assertEquals("http://127.0.0.1:8080/repo1/bundles/org.example.foo/1.2.3.qualifier", location.toString());
    }
    
    public void testReadBackBundle() throws Exception {
    	WebResource resource = Client.create().resource("http://127.0.0.1:8080/repo1/bundles/org.example.foo/1.2.3.qualifier");
    	ClientResponse response = resource.get(ClientResponse.class);
    	
    	JarInputStream stream = new JarInputStream(response.getEntityInputStream());
    	Manifest manifest = stream.getManifest();
    	
    	assertEquals("org.example.foo", manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME));
    	assertEquals("1.2.3.qualifier", manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION));
    	
    	stream.close();
    }
    
	void generateJar(OutputStream output, Map<String, String> headers, int entryCount, int entryKb) throws IOException {
		Manifest manifest = new Manifest();
		Attributes attribs = manifest.getMainAttributes();
		attribs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		for (Entry<String, String> entry : headers.entrySet()) {
			attribs.putValue(entry.getKey(), entry.getValue());
		}
		
		try {
			JarOutputStream zipStream = new JarOutputStream(output, manifest);
			zipStream.setLevel(Deflater.NO_COMPRESSION);
			
			for (int i = 0; i < entryCount; i++) {
				String name = String.format("name%05d", i);
				zipStream.putNextEntry(new ZipEntry(name));
				
				for (int j = 0 ; j < entryKb ; j++) {
					byte[] buf = new byte[1024];
					Arrays.fill(buf, (byte) 0xFF);
					zipStream.write(buf);
				}
			}
			zipStream.close();
		} finally {
			output.close();
		}
	}
}
