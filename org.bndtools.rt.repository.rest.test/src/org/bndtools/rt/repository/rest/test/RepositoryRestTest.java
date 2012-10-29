package org.bndtools.rt.repository.rest.test;

import java.io.File;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import junit.framework.TestCase;

import org.bndtools.rt.repository.api.QueryCache;
import org.bndtools.rt.repository.marshall.CapReqBuilder;
import org.bndtools.rt.repository.marshall.ResourceBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;

import aQute.bnd.service.IndexProvider;
import aQute.lib.io.IO;

public class RepositoryRestTest extends TestCase {
	
	static {
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
		}
	}

    private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

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
    
    public void testGetIndexes() throws Exception {
    	// Setup the mocks
    	MockIndexedRepository mockRepo = new MockIndexedRepository();
    	
    	List<URI> indexes = new ArrayList<URI>();
    	indexes.add(new URI("file:///Users/njbartlett/repos/index.xml"));
    	indexes.add(new URI("http://central.org/index.xml"));
    	mockRepo.setIndexes(indexes);
    	
    	// Register the mocks
		ServiceRegistration repoReg = context.registerService(new String[] { Repository.class.getName(), IndexProvider.class.getName() }, mockRepo, null);
    
		// GET the index list
		ClientResource client = new ClientResource("http://127.0.0.1:8080/repo/index");
		client.setRetryOnError(false);
		Representation rep = client.get(MediaType.APPLICATION_JSON);
		
		// Check the GET result
		assertEquals(Status.SUCCESS_OK, client.getResponse().getStatus());
		StringWriter writer = new StringWriter();
		rep.write(writer);
		String expectedOutput = IO.collect(RepositoryRestTest.class.getResourceAsStream("indexes.json")).replaceAll("\\s", "").replace("%REPO_URI%", "http://127.0.0.1:8080/repo/index/0");
		String actualOutput = writer.toString().trim();
		assertEquals(expectedOutput, actualOutput);
		
		// Tidy up
		repoReg.unregister();
    }
    
    public void testGetIndexContents() throws Exception {
    	// Setup the mocks
    	MockIndexedRepository mockRepo = new MockIndexedRepository();
    	
    	List<URI> indexes = new ArrayList<URI>();
    	indexes.add(new URI("http://central.org/index.xml"));
    	indexes.add(new File("testdata/index.xml").toURI());
    	mockRepo.setIndexes(indexes);
    	
    	// Register the mocks
		ServiceRegistration repoReg = context.registerService(new String[] { Repository.class.getName(), IndexProvider.class.getName() }, mockRepo, null);
		
		// GET the index content
		ClientResource client = new ClientResource("http://127.0.0.1:8080/repo/index/1");
		client.setRetryOnError(false);
		Representation rep = client.get();
		
		// Check the GET result
		assertEquals(Status.SUCCESS_OK, client.getResponse().getStatus());
		StringWriter writer = new StringWriter();
		rep.write(writer);
		String expectedOutput = IO.collect(new File("testdata/index.xml"));
		String actualOutput = writer.toString().trim();
		assertEquals(expectedOutput, actualOutput);
		
		// Tidy up
		repoReg.unregister();
    }
    
    public void testGetExternalIndexContentNotAllowed() throws Exception {
    	// Setup the mocks
    	MockIndexedRepository mockRepo = new MockIndexedRepository();
    	List<URI> indexes = new ArrayList<URI>();
    	indexes.add(new URI("http://central.org/index.xml"));
    	indexes.add(new File("testdata/index.xml").toURI());
    	mockRepo.setIndexes(indexes);
    	
    	// Register the mocks
		ServiceRegistration repoReg = context.registerService(new String[] { Repository.class.getName(), IndexProvider.class.getName() }, mockRepo, null);
		
		// Try to GET the index content
		ClientResource client = new ClientResource("http://127.0.0.1:8080/repo/index/0");
		client.setRetryOnError(false);
		client.setFollowingRedirects(false);
		
		client.get();
		assertEquals(303, client.getResponse().getStatus().getCode());
		assertEquals("http://central.org/index.xml", client.getResponse().getLocationRef().toString());
		
		// Tidy up
		repoReg.unregister();
    }
    
}
