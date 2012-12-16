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

import junit.framework.TestCase;

import org.bndtools.service.endpoint.Endpoint;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.restlet.data.MediaType;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;


public class RestAdapterTest extends TestCase {

	private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
	
	public void testEndpointServiceRegistered() throws Exception {
		Thread.sleep(5000);
		
		List<String> endpointUris = new ArrayList<String>();
		ServiceReference[] refs = context.getAllServiceReferences(Endpoint.class.getName(), null);
		assertNotNull(refs);
		for (ServiceReference ref : refs) {
			assertEquals("*", ref.getProperty("service.exported.interfaces"));
			endpointUris.add((String) ref.getProperty(Endpoint.URI));
		}
		Collections.sort(endpointUris);
		String actualUris = endpointUris.toString();
		
		List<String> expectedEndpoints = new ArrayList<String>(Arrays.asList(new String[] {
				"http://" + InetAddress.getLocalHost().getHostAddress() + ":18080/example1",
				"http://" + InetAddress.getLocalHost().getHostAddress() + ":18080/example3",
				"http://" + InetAddress.getLocalHost().getHostAddress() + ":18080/"
		}));
		Collections.sort(expectedEndpoints);
		String expectedUris = endpointUris.toString();
		
		assertEquals(expectedUris, actualUris);
	}

	public void testSimpleSingleton() throws Exception {
		ClientResource resource = new ClientResource("http://127.0.0.1:18080/example1/foo");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("This is an easy resource (as plain text)", output.toString());
	}
	
	public void testSingletonMissingMandatoryRef() throws Exception {
		ClientResource resource = new ClientResource("http://127.0.0.1:18080/example2/foo");
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
		Runnable mockRunnable = mock(Runnable.class);
		
		ServiceRegistration svcReg = context.registerService(Runnable.class.getName(), mockRunnable, null);
		
		ClientResource resource = new ClientResource("http://127.0.0.1:18080/example2/foo");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("This is an easy resource (as plain text)", output.toString());
		
		verify(mockRunnable).run();
		verifyNoMoreInteractions(mockRunnable);
		
		svcReg.unregister();
	}
	
	public void testSimpleClassResource() throws Exception {
		ClientResource resource = new ClientResource("http://127.0.0.1:18080/example3/foo1");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("This is an easy resource (as plain text)", output.toString());
	}
	

	public void testClassResourceDefaultAlias() throws Exception {
		ClientResource resource = new ClientResource("http://127.0.0.1:18080/foo1");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("This is an easy resource (as plain text)", output.toString());
	}
	
	public void testClassInjectionMissingMandatoryRef() throws Exception {
		ClientResource resource = new ClientResource("http://127.0.0.1:18080/example3/foo2");
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
			
			assertEquals(Runnable.class.getName(), output.toString());
		}
	}
	
	public void testClassInjectionSatisfiedMandatoryRef() throws Exception {
		Runnable mockRunnable = mock(Runnable.class);
		
		ServiceRegistration svcReg = context.registerService(Runnable.class.getName(), mockRunnable, null);
		
		ClientResource resource = new ClientResource("http://127.0.0.1:18080/example3/foo2");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("This is an easy resource (as plain text)", output.toString());
		
		verify(mockRunnable).run();
		verifyNoMoreInteractions(mockRunnable);
		
		svcReg.unregister();
	}

	public void testClassInjectionUnsatisfiedOptionalRef() throws Exception {
		ClientResource resource = new ClientResource("http://127.0.0.1:18080/example3/foo3");
		resource.setRetryOnError(false);
		
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NULL", output.toString());
	}
	
	public void testClassInjectionSatisfiedOptionalRef() throws Exception {
		Runnable mockRunnable = mock(Runnable.class);
		
		ServiceRegistration svcReg = context.registerService(Runnable.class.getName(), mockRunnable, null);
		
		ClientResource resource = new ClientResource("http://127.0.0.1:18080/example3/foo3");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NOT NULL", output.toString());
		
		svcReg.unregister();
	}

	public void testClassInjectionUnsatisfiedOptionalRefAlternateOrder() throws Exception {
		ClientResource resource = new ClientResource("http://127.0.0.1:18080/example3/foo4");
		resource.setRetryOnError(false);
		
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NULL", output.toString());
	}
	
	public void testClassInjectionSatisfiedOptionalRefAlternateOrder() throws Exception {
		Runnable mockRunnable = mock(Runnable.class);
		
		ServiceRegistration svcReg = context.registerService(Runnable.class.getName(), mockRunnable, null);
		
		ClientResource resource = new ClientResource("http://127.0.0.1:18080/example3/foo4");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NOT NULL", output.toString());
		
		svcReg.unregister();
	}
	
	public void testClassInjectionUnsatisfiedFilterRef() throws Exception {
		Runnable mockRunnable = mock(Runnable.class);
		
		ServiceRegistration svcReg = context.registerService(Runnable.class.getName(), mockRunnable, null);
		
		ClientResource resource = new ClientResource("http://127.0.0.1:18080/example3/foo5");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NULL", output.toString());
		
		svcReg.unregister();
	}
	
	public void testClassInjectionSatisfiedFilterRef() throws Exception {
		Runnable mockRunnable = mock(Runnable.class);
		
		Properties props = new Properties();
		props.put("foo", "bar");
		ServiceRegistration svcReg = context.registerService(Runnable.class.getName(), mockRunnable, props);
		
		ClientResource resource = new ClientResource("http://127.0.0.1:18080/example3/foo5");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NOT NULL", output.toString());
		
		svcReg.unregister();
	}
	
	public void testClassInjectionCollection() throws Exception {
		Runnable mockRunnable1 = mock(Runnable.class);
		Runnable mockRunnable2 = mock(Runnable.class);
		
		ServiceRegistration svcReg1 = context.registerService(Runnable.class.getName(), mockRunnable1, null);
		ServiceRegistration svcReg2 = context.registerService(Runnable.class.getName(), mockRunnable2, null);
		
		ClientResource resource = new ClientResource("http://127.0.0.1:18080/example3/foo6");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("2", output.toString());
		
		svcReg1.unregister();
		svcReg2.unregister();
	}
	
	public void testClassInjectionCollectionUnsatisfied() throws Exception {
		ClientResource resource = new ClientResource("http://127.0.0.1:18080/example3/foo6");
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
			
			assertEquals(Runnable.class.getName(), output.toString());
		}
	}
	
	public void testClassInjectionCollectionOptionalSatisfied() throws Exception {
		Runnable mockRunnable1 = mock(Runnable.class);
		Runnable mockRunnable2 = mock(Runnable.class);
		
		ServiceRegistration svcReg1 = context.registerService(Runnable.class.getName(), mockRunnable1, null);
		ServiceRegistration svcReg2 = context.registerService(Runnable.class.getName(), mockRunnable2, null);
		
		ClientResource resource = new ClientResource("http://127.0.0.1:18080/example3/foo7");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("2", output.toString());
		
		svcReg1.unregister();
		svcReg2.unregister();
	}
	
	public void testClassInjectionCollectionOptionalUnsatisfied() throws Exception {
		ClientResource resource = new ClientResource("http://127.0.0.1:18080/example3/foo7");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("0", output.toString());
	}
}
