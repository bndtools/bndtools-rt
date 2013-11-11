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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import javax.servlet.Servlet;

import org.bndtools.service.endpoint.Endpoint;
import org.example.tests.api.MyRunnable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.restlet.data.MediaType;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;


public class RestAdapterTest extends AbstractDelayedTestCase {
	
	private static final int PORT1 = 18080;
	
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
	
	public void XtestSimpleClassResource() throws Exception {
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo1");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("This is an easy resource (as plain text)", output.toString());
	}
	

	public void XtestClassResourceDefaultAlias() throws Exception {
		ClientResource resource = new ClientResource("http://" + address1 + "/foo1");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("This is an easy resource (as plain text)", output.toString());
	}
	
	public void XtestClassInjectionMissingMandatoryRef() throws Exception {
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
	}
	
	public void XtestClassInjectionSatisfiedMandatoryRef() throws Exception {
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
	}

	public void XtestClassInjectionUnsatisfiedOptionalRef() throws Exception {
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo3");
		resource.setRetryOnError(false);
		
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NULL", output.toString());
	}
	
	public void XtestClassInjectionSatisfiedOptionalRef() throws Exception {
		MyRunnable mockRunnable = mock(MyRunnable.class);
		
		ServiceRegistration svcReg = context.registerService(MyRunnable.class.getName(), mockRunnable, null);
		
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo3");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NOT NULL", output.toString());
		
		svcReg.unregister();
	}

	public void XtestClassInjectionUnsatisfiedOptionalRefAlternateOrder() throws Exception {
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo4");
		resource.setRetryOnError(false);
		
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NULL", output.toString());
	}
	
	public void XtestClassInjectionSatisfiedOptionalRefAlternateOrder() throws Exception {
		MyRunnable mockRunnable = mock(MyRunnable.class);
		
		ServiceRegistration svcReg = context.registerService(MyRunnable.class.getName(), mockRunnable, null);
		
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo4");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NOT NULL", output.toString());
		
		svcReg.unregister();
	}
	
	public void XtestClassInjectionUnsatisfiedFilterRef() throws Exception {
		MyRunnable mockRunnable = mock(MyRunnable.class);
		
		ServiceRegistration svcReg = context.registerService(MyRunnable.class.getName(), mockRunnable, null);
		
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo5");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("NULL", output.toString());
		
		svcReg.unregister();
	}
	
	public void XtestClassInjectionSatisfiedFilterRef() throws Exception {
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
	}
	
	public void XtestClassInjectionCollection() throws Exception {
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
	}
	
	public void XtestClassInjectionCollectionUnsatisfied() throws Exception {
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
	}
	
	public void XtestClassInjectionCollectionOptionalSatisfied() throws Exception {
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
	}
	
	public void XtestClassInjectionCollectionOptionalUnsatisfied() throws Exception {
		ClientResource resource = new ClientResource("http://" + address1 + "/example1/foo7");
		resource.setRetryOnError(false);
		StringWriter output = new StringWriter();
		resource.get(MediaType.TEXT_PLAIN).write(output);
		assertEquals("0", output.toString());
	}
}
