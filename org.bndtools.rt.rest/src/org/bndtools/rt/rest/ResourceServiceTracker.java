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

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

@SuppressWarnings("rawtypes")
public class ResourceServiceTracker extends ServiceTracker {
	
	static final class RegisteredResource {
		final String alias;
		final Object resource;
		RegisteredResource(String alias, Object resource) {
			this.alias = alias;
			this.resource = resource;
			
		}
	}
	
	public static final String PROP_ALIAS = "osgi.rest.alias";
	
	private static final String FILTER_STRING = "(" + PROP_ALIAS + "=*)";
	
	private final RestAppServletManager manager;
	private final LogService log;
	
	@SuppressWarnings("unchecked")
	public ResourceServiceTracker(BundleContext context, RestAppServletManager manager, LogService log) throws InvalidSyntaxException {
		super(context, createFilter(), null);
		this.manager = manager;
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
	public Object addingService(ServiceReference reference) {
		Object result = null;
		
		Object aliasObj = reference.getProperty(PROP_ALIAS);
		if (aliasObj != null && aliasObj instanceof String) {
			String alias = (String) aliasObj;
			@SuppressWarnings("unchecked")
			Object service = context.getService(reference);
			try {
				manager.addSingletons(alias, Collections.singleton(service));
				result = new RegisteredResource(alias, service);
			} catch (Exception e) {
				log.log(LogService.LOG_ERROR, String.format("Error adding resource to alias '%s'.", alias), e);
			}
		}
		
		return result;
	}
	
	@Override
	public void removedService(ServiceReference reference, Object service) {
		context.ungetService(reference);
		
		RegisteredResource registered = (RegisteredResource) service;
		try {
			manager.removeSingletons(registered.alias, Collections.singleton(registered.resource));
		} catch (Exception e) {
			log.log(LogService.LOG_ERROR, String.format("Error removing resource from alias '%s'.", registered.alias), e);
		}
	}
}
