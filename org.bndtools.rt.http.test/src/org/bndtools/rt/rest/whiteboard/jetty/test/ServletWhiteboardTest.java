package org.bndtools.rt.rest.whiteboard.jetty.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.restlet.data.MediaType;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;


public class ServletWhiteboardTest extends AbstractDelayedTest {
	
	private static final int PORT1 = 18080;
	private static final int PORT2 = 28080;
	private static final String MESSAGE = "Hello World!";

	private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
	private final String localhost;
	private final String address1;
	private final String address2;
	
	public ServletWhiteboardTest() throws Exception {
		localhost = InetAddress.getLocalHost().getHostAddress();
		address1 = localhost + ":" + PORT1;
		address2 = localhost + ":" + PORT2;
	}

	private final HttpServlet sampleServlet = new HttpServlet() {
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			ServletOutputStream stream = resp.getOutputStream();
			PrintWriter writer = new PrintWriter(stream);
			writer.print(MESSAGE);
			writer.flush();
			stream.close();
		}
	};

	public void testHttpServletRegisterAndUnregister() throws Exception {
		// Register servlet
		Properties props = new Properties();
		props.setProperty("bndtools.rt.http.alias", "/test1");
		ServiceRegistration reg = context.registerService(Servlet.class.getName(), sampleServlet, props);
		
		// Get the HTTP response
		String url = "http://localhost:" + PORT1 + "/test1";
		ClientResource resource = new ClientResource(url);
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals(MESSAGE, output.toString());

		// Unregister servlet
		reg.unregister();
		
		// Get the HTTP response again, should fail with 404
		resource = new ClientResource(url);
		resource.setRetryOnError(false);
		output = new StringWriter();
		
		try {
			resource.get().write(output);
			fail("Should have return 404 Not Found");
		} catch (ResourceException e) {
			assertEquals(404, e.getStatus().getCode());
		}
	}
	
	public void testEndpointServicesPublished() throws Exception {
		// Check no endpoint services yet...
		ServiceReference[] refs = context.getAllServiceReferences("org.bndtools.service.endpoint.Endpoint", null);
		assertNull(refs);
		
		// Register servlet with some attributes
		Properties props = new Properties();
		props.setProperty("bndtools.rt.http.alias", "/test1");
		props.setProperty("foo", "bar");
		props.put("bar", 123);
		ServiceRegistration reg = context.registerService(Servlet.class.getName(), sampleServlet, props);
		
		List<String> endpointUris = new ArrayList<String>();
		refs = context.getAllServiceReferences("org.bndtools.service.endpoint.Endpoint", null);
		assertNotNull(refs);
		for (ServiceReference ref : refs) {
			StringBuilder builder = new StringBuilder();
			
			assertEquals("*", ref.getProperty("service.exported.interfaces"));
			String[] uris = (String[]) ref.getProperty("uri");
			for (String uri : uris) {
				builder.append(uri);
				String[] keys = ref.getPropertyKeys();
				Arrays.sort(keys);
				
				for (String key : keys) {
					if (!"service.exported.interfaces".equals(key) && !"uri".equals(key) && !Constants.OBJECTCLASS.equals(key) && !Constants.SERVICE_ID.equals(key) && !"bndtools.rt.http.alias".equals(key)) {
						builder.append(';').append(key).append('=').append(ref.getProperty(key));
					}
				}
				endpointUris.add(builder.toString());
			}
		}
		Collections.sort(endpointUris);
		String actualUris = endpointUris.toString();
		
		List<String> expectedUriList = Arrays.asList(new String[] {
				"http://" + localhost + ":" + PORT1 + "/test1;bar=123;foo=bar"
		});
		Collections.sort(expectedUriList);
		String expectedUris = expectedUriList.toString();

		System.out.println("EXPECTED ENDPOINTS: " + expectedUris);
		System.out.println("ACTUAL ENDPOINTS  : " + actualUris);
		assertEquals(expectedUris, actualUris);
		
		reg.unregister();
	}
}
