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
package org.bndtools.rt.rest;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import com.sun.jersey.spi.container.servlet.ServletContainer;

public class ResourceServiceTracker extends ServiceTracker<Object, ServiceRegistration<Servlet>> {
	
	static final class RegisteredResource {
		final String alias;
		final Object resource;
		RegisteredResource(String alias, Object resource) {
			this.alias = alias;
			this.resource = resource;
		}
	}
	
	public static final String PROP_ALIAS = "osgi.rest.alias";
	public static final String PROP_ENDPOINT_NAME = "osgi.rest.endpointName";
	
	private static final String FILTER_STRING = "(" + PROP_ALIAS + "=*)";
	
	private final BundleContext context;
	private final LogService log;
	
	public ResourceServiceTracker(BundleContext context, LogService log) throws InvalidSyntaxException {
		super(context, createFilter(), null);

		this.context = context;
		this.log = log;
	}
	
	private static Filter createFilter() {
		try {
			return FrameworkUtil.createFilter(FILTER_STRING);
		} catch (InvalidSyntaxException e) {
			// Can't happen...
			throw new RuntimeException(e);
		}
	}

	@Override
	public ServiceRegistration<Servlet> addingService(ServiceReference<Object> reference) {
		Object aliasObj = reference.getProperty(PROP_ALIAS);
		if (aliasObj != null && aliasObj instanceof String) {
			String alias = (String) aliasObj;
			
			Object service = context.getService(reference);
			ImmutableApplication app = ImmutableApplication.empty().addSingletons(Collections.singleton(service));
			ServletContainer servlet = new ServletContainer(app);
			
			Dictionary<String, Object> properties = new Hashtable<String, Object>();
			properties.put("bndtools.rt.http.alias", alias);
			
			String[] propertyKeys = reference.getPropertyKeys();
			for (String key : propertyKeys) {
				if (!Constants.OBJECTCLASS.equals(key) && !Constants.SERVICE_ID.equals(key) && !ComponentConstants.COMPONENT_ID.equals(key) && !PROP_ALIAS.equals(key))
					properties.put(key, reference.getProperty(key));
			}
			
			ServiceRegistration<Servlet> registration = context.registerService(Servlet.class, servlet, properties);
			
			return registration;
		}
		return null;
	}
	
	@Override
	public void removedService(ServiceReference<Object> reference, ServiceRegistration<Servlet> registration) {
		registration.unregister();
		context.ungetService(reference);
	}
}
