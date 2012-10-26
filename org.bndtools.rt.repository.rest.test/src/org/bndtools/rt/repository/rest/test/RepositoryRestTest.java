package org.bndtools.rt.repository.rest.test;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

import aQute.lib.io.IO;

public class RepositoryRestTest extends TestCase {

    private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    @SuppressWarnings("unchecked")
	public void testPostQueryAndReadBack() throws Exception {
    	Thread.sleep(3000);
    	
    	UUID uuid = UUID.randomUUID();
    	String queryText = IO.collect(RepositoryRestTest.class.getResourceAsStream("query1.json"));
    	
    	MockQueryCache mockCache = new MockQueryCache();
    	mockCache.setUuid(uuid);
    	ServiceRegistration cacheReg = context.registerService(QueryCache.class.getName(), mockCache, null);
    	ServiceRegistration repoReg = context.registerService(Repository.class.getName(), new MockRepository(), null);
    	
    	ClientResource client = new ClientResource("http://127.0.0.1:8080/repo/query");
    	client.setRetryOnError(false);
    	
    	client.post(new StringRepresentation(queryText, MediaType.APPLICATION_JSON));
    	Response response = client.getResponse();
    	String location = response.getLocationRef().toString();
    	
    	assertEquals(Status.SUCCESS_CREATED.getCode(), response.getStatus().getCode());
		assertEquals("http://127.0.0.1:8080/repo/query/" + uuid, location);
		
		// Get the new resource
		Collection<Requirement> mockResponse = Collections.singletonList(new CapReqBuilder("osgi.extender").addDirective("filter", "(osgi.extender=osgi.ds)").buildSyntheticRequirement());
		mockCache.setRequirements(mockResponse);
		
		client = new ClientResource(location);
		client.setRetryOnError(false);
		Representation rep = client.get(MediaType.APPLICATION_JSON);
		StringWriter writer = new StringWriter();
		rep.write(writer);
		
		assertEquals(queryText.trim(), writer.toString().trim());
    	
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
    
}
