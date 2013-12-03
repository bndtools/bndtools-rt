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
package org.example.tests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.servlet.Servlet;

import org.bndtools.service.endpoint.Endpoint;
import org.example.tests.api.MyRunnable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.restlet.data.MediaType;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import aQute.lib.io.IO;


public class RestAdapterTest extends AbstractDelayedTestCase {
	
	private static final int PORT1 = 18080;
	private static final int HTTPS_PORT = 18443;
	
	private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
	
	private final String localhost;
	private final String address1;
	
	public RestAdapterTest() throws Exception {
		localhost = InetAddress.getLocalHost().getHostAddress();
		address1 = localhost + ":" + PORT1;
	}
	
	public void testNothingRegisteredAtFirst() throws Exception {
		ServiceReference[] refs = context.getAllServiceReferences(Endpoint.class.getName(), null);
		assertNull(refs);
		
		ClientResource resource = new ClientResource("http://" + address1 + "/");
		resource.setRetryOnError(false);
		try {
			resource.get().write(new StringWriter());
			fail("Should throw ResourceException");
		} catch (ResourceException e) {
			assertEquals(404, e.getStatus().getCode());
		}
	}
	
	public void testSimpleSingleton() throws Exception {
		// Register the singleton service
		Dictionary<String, Object> svcProps = new Hashtable<String, Object>();
		svcProps.put("osgi.rest.alias", "/test1");
		svcProps.put("foo", "bar");
		ServiceRegistration svcReg = context.registerService(Object.class.getName(), new SingletonServiceResource1(), svcProps);
		
		// Check for advertised Servlet service
		ServiceReference[] refs = context.getAllServiceReferences(Servlet.class.getName(), null);
		assertNotNull(refs);
		assertEquals(1, refs.length);
		assertEquals("/test1", refs[0].getProperty("bndtools.rt.http.alias"));
		assertEquals("bar", refs[0].getProperty("foo"));
		
		// Check for advertised Endpoint service
		ServiceReference[] endpointRefs = context.getAllServiceReferences(Endpoint.class.getName(), null);
		assertNotNull(endpointRefs);
		assertEquals(1, endpointRefs.length);
		assertEquals("*", endpointRefs[0].getProperty("service.exported.interfaces"));
		assertEquals("bar", endpointRefs[0].getProperty("foo"));
		
		// Connect by HTTP
		ClientResource resource = new ClientResource("http://" + address1 + "/test1/foo");
		resource.setRetryOnError(false);
		StringWriter writer = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(writer);
		assertEquals("Hello World", writer.toString());

		// Unregister
		svcReg.unregister();
		
		// Check it's gone
		refs = context.getAllServiceReferences(Servlet.class.getName(), null);
		assertNull(refs);
		endpointRefs = context.getAllServiceReferences(Endpoint.class.getName(), null);
		assertNull(endpointRefs);

		// Check I can't connect
		resource = new ClientResource("http://" + address1 + "/test1/foo");
		resource.setRetryOnError(false);
		try {
			resource.get().write(new StringWriter());
			fail("Should throw ResourceException");
		} catch (ResourceException e) {
			assertEquals(404, e.getStatus().getCode());
		}
	}
	
	public void testSecuredSingleton() throws Exception {
		// Register the singleton service
		Dictionary<String, Object> svcProps = new Hashtable<String, Object>();
		svcProps.put("osgi.rest.alias", "/test2");
		svcProps.put("filter", "(confidential=true)");
		ServiceRegistration svcReg = context.registerService(Object.class.getName(), new SingletonServiceResource1(), svcProps);
		
		// Check for advertised Servlet service
		ServiceReference[] refs = context.getAllServiceReferences(Servlet.class.getName(), null);
		assertNotNull(refs);
		assertEquals(1, refs.length);
		assertEquals("/test2", refs[0].getProperty("bndtools.rt.http.alias"));
		assertEquals("(confidential=true)", refs[0].getProperty("filter"));
		
		// Check for advertised Endpoint service
		ServiceReference[] endpointRefs = context.getAllServiceReferences(Endpoint.class.getName(), null);
		assertNotNull(endpointRefs);
		assertEquals(1, endpointRefs.length);
		assertEquals("*", endpointRefs[0].getProperty("service.exported.interfaces"));

		// Connect by HTTPS
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, new TrustManager[] { new NoopTrustManager() }, null);
		
		HttpsURLConnection connection = (HttpsURLConnection) new URL("https://" + localhost + ":" + HTTPS_PORT + "/test2/foo").openConnection();
		connection.setSSLSocketFactory(sslContext.getSocketFactory());
		connection.setHostnameVerifier(new NoopHostnameVerifier());
		connection.setRequestProperty("Accept", "text/plain");
		String output = IO.collect(connection.getInputStream());
		assertEquals("Hello World", output);

		// Clean up
		svcReg.unregister();
	}
	
	private Bundle installAndStart(File file) throws Exception {
		Bundle bundle = context.installBundle(file.getAbsoluteFile().toURL().toString());
		bundle.start();
		return bundle;
	}

	public void testSimpleClassResource() throws Exception {
		// Install & start bundle
		Bundle exampleBundle = installAndStart(new File("generated/org.bndtools.rt.rest.test.example1.jar"));
		
		// Check for the servlet service
		ServiceReference[] refs = context.getAllServiceReferences(Servlet.class.getName(), null);
		assertNotNull(refs);
		assertEquals(1, refs.length);
		assertEquals("/example1", refs[0].getProperty("bndtools.rt.http.alias"));
		
		// Check for advertised Endpoint service
		ServiceReference[] endpointRefs = context.getAllServiceReferences(Endpoint.class.getName(), null);
		assertNotNull(endpointRefs);
		assertEquals(1, endpointRefs.length);
		assertEquals("*", endpointRefs[0].getProperty("service.exported.interfaces"));
		
		// Connect by HTTP
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo1");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("This is an easy resource (as plain text)", output.toString());
		
		// Uninstall bundle
		exampleBundle.uninstall();
	}

	public void testClassResourceDefaultAlias() throws Exception {
		Bundle exampleBundle = installAndStart(new File("generated/org.bndtools.rt.rest.test.example2.jar"));
		
		// Check for the servlet service
		ServiceReference[] refs = context.getAllServiceReferences(Servlet.class.getName(), null);
		assertNotNull(refs);
		assertEquals(1, refs.length);
		assertEquals("/", refs[0].getProperty("bndtools.rt.http.alias"));
		
		// Check for advertised Endpoint service
		ServiceReference[] endpointRefs = context.getAllServiceReferences(Endpoint.class.getName(), null);
		assertNotNull(endpointRefs);
		assertEquals(1, endpointRefs.length);
		assertEquals("*", endpointRefs[0].getProperty("service.exported.interfaces"));
		
		// Connect by HTTP
		ClientResource resource = new ClientResource("http://" + address1 + "/foo1");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("This is an easy resource (as plain text)", output.toString());

		// Clean up
		exampleBundle.uninstall();
	}
	
	public void testClassInjectionMissingMandatoryRef() throws Exception {
		Bundle exampleBundle = installAndStart(new File("generated/org.bndtools.rt.rest.test.example1.jar"));
		
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo2");
		resource.setRetryOnError(false);
		try {
			resource.get(MediaType.TEXT_PLAIN);
			fail("Should fail with ResourceException");
		} catch (ResourceException e) {
			// expected
			assertEquals(503, e.getStatus().getCode());
			assertEquals(MediaType.TEXT_PLAIN, resource.getResponseEntity().getMediaType());
			
			StringWriter output = new StringWriter();
			resource.getResponseEntity().write(output);
			
			assertEquals(MyRunnable.class.getName(), output.toString());
		}
		
		exampleBundle.uninstall();
	}
	
	public void testClassInjectionSatisfiedMandatoryRef() throws Exception {
		Bundle exampleBundle = installAndStart(new File("generated/org.bndtools.rt.rest.test.example1.jar"));
		
		MyRunnable mockRunnable = mock(MyRunnable.class);
		
		ServiceRegistration svcReg = context.registerService(MyRunnable.class.getName(), mockRunnable, null);
		
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo2");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("This is an easy resource (as plain text)", output.toString());
		
		verify(mockRunnable).run();
		verifyNoMoreInteractions(mockRunnable);
		
		svcReg.unregister();
		exampleBundle.uninstall();
	}

	public void testClassInjectionUnsatisfiedOptionalRef() throws Exception {
		Bundle exampleBundle = installAndStart(new File("generated/org.bndtools.rt.rest.test.example1.jar"));
		
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo3");
		resource.setRetryOnError(false);
		
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NULL", output.toString());
		exampleBundle.uninstall();
	}
	
	public void testClassInjectionSatisfiedOptionalRef() throws Exception {
		Bundle exampleBundle = installAndStart(new File("generated/org.bndtools.rt.rest.test.example1.jar"));
		
		MyRunnable mockRunnable = mock(MyRunnable.class);
		
		ServiceRegistration svcReg = context.registerService(MyRunnable.class.getName(), mockRunnable, null);
		
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo3");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NOT NULL", output.toString());
		
		svcReg.unregister();
		exampleBundle.uninstall();
	}

	public void testClassInjectionUnsatisfiedOptionalRefAlternateOrder() throws Exception {
		Bundle exampleBundle = installAndStart(new File("generated/org.bndtools.rt.rest.test.example1.jar"));
		
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo4");
		resource.setRetryOnError(false);
		
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NULL", output.toString());
		exampleBundle.uninstall();
	}
	
	public void testClassInjectionSatisfiedOptionalRefAlternateOrder() throws Exception {
		Bundle exampleBundle = installAndStart(new File("generated/org.bndtools.rt.rest.test.example1.jar"));
		
		MyRunnable mockRunnable = mock(MyRunnable.class);
		
		ServiceRegistration svcReg = context.registerService(MyRunnable.class.getName(), mockRunnable, null);
		
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo4");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NOT NULL", output.toString());
		
		svcReg.unregister();
		exampleBundle.uninstall();
	}
	
	public void testClassInjectionUnsatisfiedFilterRef() throws Exception {
		Bundle exampleBundle = installAndStart(new File("generated/org.bndtools.rt.rest.test.example1.jar"));
		
		MyRunnable mockRunnable = mock(MyRunnable.class);
		
		ServiceRegistration svcReg = context.registerService(MyRunnable.class.getName(), mockRunnable, null);
		
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo5");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NULL", output.toString());
		
		svcReg.unregister();
		exampleBundle.uninstall();
	}
	
	public void testClassInjectionSatisfiedFilterRef() throws Exception {
		Bundle exampleBundle = installAndStart(new File("generated/org.bndtools.rt.rest.test.example1.jar"));
		
		MyRunnable mockRunnable = mock(MyRunnable.class);
		
		Properties props = new Properties();
		props.put("foo", "bar");
		ServiceRegistration svcReg = context.registerService(MyRunnable.class.getName(), mockRunnable, props);
		
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo5");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NOT NULL", output.toString());
		
		svcReg.unregister();
		exampleBundle.uninstall();
	}
	
	public void testClassInjectionCollection() throws Exception {
		Bundle exampleBundle = installAndStart(new File("generated/org.bndtools.rt.rest.test.example1.jar"));
		
		MyRunnable mockRunnable1 = mock(MyRunnable.class);
		MyRunnable mockRunnable2 = mock(MyRunnable.class);
		
		ServiceRegistration svcReg1 = context.registerService(MyRunnable.class.getName(), mockRunnable1, null);
		ServiceRegistration svcReg2 = context.registerService(MyRunnable.class.getName(), mockRunnable2, null);
		
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo6");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("2", output.toString());
		
		svcReg1.unregister();
		svcReg2.unregister();
		exampleBundle.uninstall();
	}
	
	public void XtestClassInjectionCollectionUnsatisfied() throws Exception {
		Bundle exampleBundle = installAndStart(new File("generated/org.bndtools.rt.rest.test.example1.jar"));
		
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo6");
		resource.setRetryOnError(false);
		try {
			resource.get(MediaType.TEXT_PLAIN);
			fail("Should fail with ResourceException");
		} catch (ResourceException e) {
			// expected
			assertEquals(503, e.getStatus().getCode());
			assertEquals(MediaType.TEXT_PLAIN, resource.getResponseEntity().getMediaType());

			StringWriter output = new StringWriter();
			resource.getResponseEntity().write(output);
			
			assertEquals(MyRunnable.class.getName(), output.toString());
		}
		exampleBundle.uninstall();
	}
	
	public void testClassInjectionCollectionOptionalSatisfied() throws Exception {
		Bundle exampleBundle = installAndStart(new File("generated/org.bndtools.rt.rest.test.example1.jar"));
		
		MyRunnable mockRunnable1 = mock(MyRunnable.class);
		MyRunnable mockRunnable2 = mock(MyRunnable.class);
		
		ServiceRegistration svcReg1 = context.registerService(MyRunnable.class.getName(), mockRunnable1, null);
		ServiceRegistration svcReg2 = context.registerService(MyRunnable.class.getName(), mockRunnable2, null);
		
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo7");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("2", output.toString());
		
		svcReg1.unregister();
		svcReg2.unregister();
		exampleBundle.uninstall();
	}
	
	public void testClassInjectionCollectionOptionalUnsatisfied() throws Exception {
		Bundle exampleBundle = installAndStart(new File("generated/org.bndtools.rt.rest.test.example1.jar"));
		
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo7");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("0", output.toString());
		exampleBundle.uninstall();
	}
}
