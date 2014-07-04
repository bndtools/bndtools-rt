package org.bndtools.rt.http.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import aQute.lib.io.IO;


public class ServletWhiteboardTest extends AbstractDelayedTest {
	
	private static final int PORT1 = 18080;
	private static final int PORT2 = 18443;
	private static final String FILTER_PREFIX = "FILTERED!!!\n";
	private static final String MESSAGE = "Hello World!";

	private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
	private final String localhost;
	
	public ServletWhiteboardTest() throws Exception {
		localhost = InetAddress.getLocalHost().getHostAddress();
	}

	private final HttpServlet sampleServlet = new HttpServlet() {
		private static final long serialVersionUID = 1L;
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			ServletOutputStream stream = resp.getOutputStream();
			PrintWriter writer = new PrintWriter(stream);
			writer.print(MESSAGE);
			writer.flush();
			stream.close();
		}
	};
	
	private final Filter sampleFilter = new Filter() {

		@Override
		public void destroy() {}

		@Override
		public void doFilter(ServletRequest req, ServletResponse resp,
				FilterChain chain) throws IOException, ServletException {
			ServletOutputStream stream = resp.getOutputStream();
			PrintWriter writer = new PrintWriter(stream);
			writer.print(FILTER_PREFIX);
			writer.flush();
			chain.doFilter(req, resp);
		}

		@Override
		public void init(FilterConfig arg0) throws ServletException {}
		
	};

	public void testHttpServletRegisterAndUnregister() throws Exception {
		// Register servlet
		Properties props = new Properties();
		props.setProperty("bndtools.rt.http.alias", "/test1");
		ServiceRegistration reg = context.registerService(Servlet.class.getName(), sampleServlet, props);
		
		// Get the HTTP response
		String url = "http://localhost:" + PORT1 + "/test1";
		String output = IO.collect(new URL(url));
		assertEquals(MESSAGE, output);

		// Unregister servlet
		reg.unregister();
		
		// Get the HTTP response again, should fail with 404
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		int responseCode = connection.getResponseCode();
		assertEquals(404, responseCode);
	}

	public void testFilteredHttpServletRegisterAndUnregister() throws Exception {
		// Register servlet
		Properties props = new Properties();
		props.setProperty("bndtools.rt.http.alias", "/test1");
		ServiceRegistration reg = context.registerService(Servlet.class.getName(), sampleServlet, props);
		ServiceRegistration reg2 = context.registerService(Filter.class.getName(), sampleFilter, props);
		
		// Get the HTTP response
		String url = "http://localhost:" + PORT1 + "/test1";
		String output = IO.collect(new URL(url));
		assertEquals(FILTER_PREFIX + MESSAGE, output);
		
		// Unregister filter and check output changes
		reg2.unregister();
		
		output = IO.collect(new URL(url));
		assertEquals(MESSAGE, output);

		// Unregister servlet
		reg.unregister();
		
		// Get the HTTP response again, should fail with 404
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		int responseCode = connection.getResponseCode();
		assertEquals(404, responseCode);
	}
	
	public void testEndpointServicesPublished() throws Exception {
		// Check no endpoint services yet...
		ServiceReference[] refs = context.getAllServiceReferences("org.bndtools.service.endpoint.Endpoint", null);
		assertNull(refs);
		
		// Register servlet with some attributes
		Properties props = new Properties();
		props.setProperty("bndtools.rt.http.alias", "/test1");
		
		//
		// This is unsecure, so check if we can require this. This
		// used to fail.
		//
		
		props.setProperty("filter", "(confidential=false)");
		
		props.setProperty("foo", "bar");
		props.put("bar", 123);
		ServiceRegistration reg = context.registerService(Servlet.class.getName(), sampleServlet, props);
		
		refs = context.getAllServiceReferences("org.bndtools.service.endpoint.Endpoint", null);
		assertNotNull(refs);
		String actualUris = formatEndpointServices(refs);
		
		String expectedUri = "http://" + localhost + ":" + PORT1 + "/test1;bar=123;foo=bar";
		String expectedUris = formatEndpointServices(new String[] {
				expectedUri
		});

		
		System.out.println("EXPECTED ENDPOINTS: " + expectedUris);
		System.out.println("ACTUAL ENDPOINTS  : " + actualUris);

		//
		// The following test did not work when you had multiple interfaces
		//		assertEquals(expectedUris, actualUris);
		//
		// Now just verifies that our URL is actually there
		//
		assertTrue( actualUris.contains(expectedUri));
		
		reg.unregister();
	}

	private static String formatEndpointServices(String[] array) {
		List<String> expectedUriList = Arrays.asList(array);
		Collections.sort(expectedUriList);
		return expectedUriList.toString();
	}
	
	private static String formatEndpointServices(ServiceReference[] refs) {
		List<String> endpointUris = new ArrayList<String>();
		for (ServiceReference ref : refs) {
			
			assertEquals("*", ref.getProperty("service.exported.interfaces"));
			String[] uris = (String[]) ref.getProperty("uri");
			for (String uri : uris) {
				StringBuilder builder = new StringBuilder();
				builder.append(uri);
				String[] keys = ref.getPropertyKeys();
				Arrays.sort(keys);
				
				for (String key : keys) {
					if (!"service.exported.interfaces".equals(key) && !"uri".equals(key) && !Constants.OBJECTCLASS.equals(key) && !Constants.SERVICE_ID.equals(key)) {
						builder.append(';').append(key).append('=').append(ref.getProperty(key));
					}
				}
				endpointUris.add(builder.toString());
			}
		}
		Collections.sort(endpointUris);
		return endpointUris.toString();
	}
	
	public void testSecuredServlet() throws Exception {
		String uri = "https://" + localhost + ":" + PORT2 + "/test2";
		ServiceReference[] refs;
		
		Properties props = new Properties();
		props.setProperty("bndtools.rt.http.alias", "/test2");
		props.setProperty("filter", "(confidential=true)");
		ServiceRegistration reg = context.registerService(Servlet.class.getName(), sampleServlet, props);
		
		refs = context.getAllServiceReferences("org.bndtools.service.endpoint.Endpoint", null);
		assertNotNull(refs);
		
		String actualUris = formatEndpointServices(refs);
		String expectedUris = formatEndpointServices(new String[] { uri });
		
		System.out.println("EXPECTED ENDPOINTS: " + expectedUris);
		System.out.println("ACTUAL ENDPOINTS  : " + actualUris);
		
		//
		// The following test did not work when you had multiple interfaces
		//		assertEquals(expectedUris, actualUris);
		//
		// Now just verifies that our URL is actually there
		//
		assertTrue( actualUris.contains(uri));
		
		final SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, new TrustManager[] { new NoopTrustManager() }, null);
		
		HttpsURLConnection connection = (HttpsURLConnection) new URL(uri).openConnection();
		connection.setSSLSocketFactory(sslContext.getSocketFactory());
		connection.setHostnameVerifier(new NoopHostnameVerifier());

		String output = IO.collect(connection.getInputStream());
		assertEquals(MESSAGE, output);

		reg.unregister();
	}
	
	public void testRegisterResource() throws Exception {
		// Register servlet
		Properties props = new Properties();
		props.setProperty("bndtools.rt.http.alias", "/test2");
		props.setProperty("bndtools.rt.http.resource.prefix", "/static");
		ServiceRegistration reg = context.registerService(Servlet.class.getName(), sampleServlet, props);
		
		// Get the HTTP response
		String url = "http://localhost:" + PORT1 + "/test2/ulysses.txt";
		String output = IO.collect(new URL(url));
		assertEquals("Stately, plump Buck Mulligan...", output);

		// Unregister servlet
		reg.unregister();
		
		// Get the HTTP response again, should fail with 404
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		int responseCode = connection.getResponseCode();
		assertEquals(404, responseCode);
	}
}
