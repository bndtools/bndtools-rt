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

import java.io.StringWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.bndtools.service.endpoint.Endpoint;
import org.example.tests.api.MyRunnable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.restlet.data.MediaType;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;


public class RestAdapterTest extends TestCase {
	
	private static final int PORT = 18080;
	private static final AtomicBoolean initialised = new AtomicBoolean(false);
	
	private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
	private final String address;
	
	public RestAdapterTest() throws Exception {
		address = InetAddress.getLocalHost().getHostAddress() + ":" + PORT;
	}
	
	@Override
	protected void setUp() throws Exception {
		boolean needToWait = initialised.compareAndSet(false, true);
		if (needToWait) {
			System.out.println("Tests1 waiting 10 seconds for system to settle.");
			Thread.sleep(10000);
			System.out.println("Waiting done, proceeding with tests");
		}
	}
	
	public void testEndpointServiceRegistered() throws Exception {
		Thread.sleep(5000);
		
		List<String> endpointUris = new ArrayList<String>();
		ServiceReference[] refs = context.getAllServiceReferences(Endpoint.class.getName(), null);
		assertNotNull(refs);
		for (ServiceReference ref : refs) {
			assertEquals("*", ref.getProperty("service.exported.interfaces"));
			String[] uris = (String[]) ref.getProperty(Endpoint.URI);
			for (String uri : uris)
				endpointUris.add(uri);
		}
		Collections.sort(endpointUris);
		String actualUris = endpointUris.toString();
		
		List<String> expectedEndpoints = new ArrayList<String>(Arrays.asList(new String[] {
				"http://" + address + "/example1",
				"http://" + address,
				"http://" + address + "/singleton1"
		}));
		Collections.sort(expectedEndpoints);
		String expectedUris = expectedEndpoints.toString();
		
		assertEquals(expectedUris, actualUris);
	}

	public void testSimpleSingleton() throws Exception {
		ClientResource resource = new ClientResource("http://" + address + "/singleton1/foo");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("This is an easy resource (as plain text)", output.toString());
	}
	
	public void XtestSingletonMissingMandatoryRef() throws Exception {
		ClientResource resource = new ClientResource("http://" + address + "/singleton2/foo");
		resource.setRetryOnError(false);
		try {
			resource.get(MediaType.TEXT_PLAIN);
			fail("Should fail with ResourceException");
		} catch (ResourceException e) {
			// expected
			assertEquals(404, e.getStatus().getCode());
		}
	}
	
	public void testSingletonWithSatisfiedMandatoryRef() throws Exception {
		MyRunnable mockRunnable = mock(MyRunnable.class);
		
		ServiceRegistration svcReg = context.registerService(MyRunnable.class.getName(), mockRunnable, null);
		
		ClientResource resource = new ClientResource("http://" + address + "/singleton2/foo");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("This is an easy resource (as plain text)", output.toString());
		
		verify(mockRunnable).run();
		verifyNoMoreInteractions(mockRunnable);
		
		svcReg.unregister();
	}
	
	public void testSimpleClassResource() throws Exception {
		ClientResource resource = new ClientResource("http://" + address + "/example1/foo1");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("This is an easy resource (as plain text)", output.toString());
	}
	

	public void testClassResourceDefaultAlias() throws Exception {
		ClientResource resource = new ClientResource("http://" + address + "/foo1");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("This is an easy resource (as plain text)", output.toString());
	}
	
	public void testClassInjectionMissingMandatoryRef() throws Exception {
		ClientResource resource = new ClientResource("http://" + address + "/example1/foo2");
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
	}
	
	public void testClassInjectionSatisfiedMandatoryRef() throws Exception {
		MyRunnable mockRunnable = mock(MyRunnable.class);
		
		ServiceRegistration svcReg = context.registerService(MyRunnable.class.getName(), mockRunnable, null);
		
		ClientResource resource = new ClientResource("http://" + address + "/example1/foo2");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("This is an easy resource (as plain text)", output.toString());
		
		verify(mockRunnable).run();
		verifyNoMoreInteractions(mockRunnable);
		
		svcReg.unregister();
	}

	public void testClassInjectionUnsatisfiedOptionalRef() throws Exception {
		ClientResource resource = new ClientResource("http://" + address + "/example1/foo3");
		resource.setRetryOnError(false);
		
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NULL", output.toString());
	}
	
	public void testClassInjectionSatisfiedOptionalRef() throws Exception {
		MyRunnable mockRunnable = mock(MyRunnable.class);
		
		ServiceRegistration svcReg = context.registerService(MyRunnable.class.getName(), mockRunnable, null);
		
		ClientResource resource = new ClientResource("http://" + address + "/example1/foo3");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NOT NULL", output.toString());
		
		svcReg.unregister();
	}

	public void testClassInjectionUnsatisfiedOptionalRefAlternateOrder() throws Exception {
		ClientResource resource = new ClientResource("http://" + address + "/example1/foo4");
		resource.setRetryOnError(false);
		
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NULL", output.toString());
	}
	
	public void testClassInjectionSatisfiedOptionalRefAlternateOrder() throws Exception {
		MyRunnable mockRunnable = mock(MyRunnable.class);
		
		ServiceRegistration svcReg = context.registerService(MyRunnable.class.getName(), mockRunnable, null);
		
		ClientResource resource = new ClientResource("http://" + address + "/example1/foo4");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NOT NULL", output.toString());
		
		svcReg.unregister();
	}
	
	public void testClassInjectionUnsatisfiedFilterRef() throws Exception {
		MyRunnable mockRunnable = mock(MyRunnable.class);
		
		ServiceRegistration svcReg = context.registerService(MyRunnable.class.getName(), mockRunnable, null);
		
		ClientResource resource = new ClientResource("http://" + address + "/example1/foo5");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NULL", output.toString());
		
		svcReg.unregister();
	}
	
	public void testClassInjectionSatisfiedFilterRef() throws Exception {
		MyRunnable mockRunnable = mock(MyRunnable.class);
		
		Properties props = new Properties();
		props.put("foo", "bar");
		ServiceRegistration svcReg = context.registerService(MyRunnable.class.getName(), mockRunnable, props);
		
		ClientResource resource = new ClientResource("http://" + address + "/example1/foo5");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NOT NULL", output.toString());
		
		svcReg.unregister();
	}
	
	public void testClassInjectionCollection() throws Exception {
		MyRunnable mockRunnable1 = mock(MyRunnable.class);
		MyRunnable mockRunnable2 = mock(MyRunnable.class);
		
		ServiceRegistration svcReg1 = context.registerService(MyRunnable.class.getName(), mockRunnable1, null);
		ServiceRegistration svcReg2 = context.registerService(MyRunnable.class.getName(), mockRunnable2, null);
		
		ClientResource resource = new ClientResource("http://" + address + "/example1/foo6");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("2", output.toString());
		
		svcReg1.unregister();
		svcReg2.unregister();
	}
	
	public void testClassInjectionCollectionUnsatisfied() throws Exception {
		ClientResource resource = new ClientResource("http://" + address + "/example1/foo6");
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
	}
	
	public void testClassInjectionCollectionOptionalSatisfied() throws Exception {
		MyRunnable mockRunnable1 = mock(MyRunnable.class);
		MyRunnable mockRunnable2 = mock(MyRunnable.class);
		
		ServiceRegistration svcReg1 = context.registerService(MyRunnable.class.getName(), mockRunnable1, null);
		ServiceRegistration svcReg2 = context.registerService(MyRunnable.class.getName(), mockRunnable2, null);
		
		ClientResource resource = new ClientResource("http://" + address + "/example1/foo7");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("2", output.toString());
		
		svcReg1.unregister();
		svcReg2.unregister();
	}
	
	public void testClassInjectionCollectionOptionalUnsatisfied() throws Exception {
		ClientResource resource = new ClientResource("http://" + address + "/example1/foo7");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("0", output.toString());
	}
}
