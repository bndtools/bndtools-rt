package org.bndtools.rt.repository.rest.test;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import junit.framework.TestCase;

import org.bndtools.rt.repository.api.QueryCache;
import org.bndtools.rt.repository.marshall.CapReqBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;

import aQute.lib.io.IO;

public class RepositoryRestTest extends TestCase {

    private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    @SuppressWarnings("unchecked")
	public void testPostQuery() throws Exception {
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
    
}
