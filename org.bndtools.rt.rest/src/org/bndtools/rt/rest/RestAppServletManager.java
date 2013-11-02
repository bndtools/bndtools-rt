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

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;

import org.eclipse.jetty.servlet.ServletHandler;
import org.osgi.framework.BundleContext;

public class RestAppServletManager {

	private final Map<String, ResourceRegistration> registrationMap = new HashMap<String, ResourceRegistration>();
	
	private final BundleContext context;
	private final List<URI> endpointAddresses;
	private final ServletManager servletMgr;

	
	public RestAppServletManager(BundleContext context, ServletHandler servletHandler, List<URI> endpointAddresses) {
		this.context = context;
		this.endpointAddresses = endpointAddresses;
		this.servletMgr = new ServletManager(servletHandler);
	}
	
	public synchronized void addSingletons(String alias, Set<Object> newResources, String endpointName) throws ServletException {
		ResourceRegistration registration;
		registration = registrationMap.get(alias);
		if (registration == null) {
			registration = ResourceRegistration.create(alias, context, servletMgr, endpointAddresses);
			registrationMap.put(alias, registration);
		}
		registration.addSingletons(newResources, endpointName);
	}
	
	public synchronized void addClasses(String alias, Set<Class<?>> newClasses, String endpointName) throws ServletException {
		ResourceRegistration registration = registrationMap.get(alias);
		if (registration == null) {
			registration = ResourceRegistration.create(alias, context, servletMgr, endpointAddresses);
			registrationMap.put(alias, registration);
		}
		registration.addClasses(newClasses, endpointName);
	}

	public synchronized void removeSingletons(String alias, final Collection<? extends Object> removedResources, String endpointName) throws ServletException {
		ResourceRegistration registration = registrationMap.get(alias);
		if (registration != null)
			registration.removeSingletons(removedResources, endpointName);
	}

	public synchronized void removeClasses(String alias, final Collection<Class<?>> removedClasses, String endpointName) throws ServletException {
		ResourceRegistration registration = registrationMap.get(alias);
		if (registration != null)
			registration.removeClasses(removedClasses, endpointName);
	}
	
	public synchronized void deleteAll() {
		for (Entry<String, ResourceRegistration> entry : registrationMap.entrySet()) {
			entry.getValue().delete();
		}
	}

}
