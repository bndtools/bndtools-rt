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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.ws.rs.core.Application;

import org.bndtools.service.endpoint.Endpoint;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import com.sun.jersey.spi.container.servlet.ServletContainer;

public class RestAppServletManager {

	private static final String WILDCARD = "*";

	private final Map<String, ImmutableApplication> appMap = new HashMap<String, ImmutableApplication>();
	private final Map<String, ServletHolder> servletMap = new LinkedHashMap<String, ServletHolder>();
	private final Map<String, ServiceRegistration<?>> endpointRegistrations = new HashMap<String, ServiceRegistration<?>>();
	
	private final BundleContext context;
	private final ServletHandler servletHandler;
	private final ImmutableApplication defaultApplication;
	private final List<URI> endpointAddresses;
	
	public RestAppServletManager(BundleContext context, ServletHandler servletHandler, List<URI> endpointAddresses) {
		this.context = context;
		this.servletHandler = servletHandler;
		this.endpointAddresses = endpointAddresses;
		
		Set<Class<?>> defaultClasses = new HashSet<Class<?>>();
		defaultClasses.add(InjectAnnotationInjectableProvider.class);
		defaultClasses.add(OptionalAnnotationInjectableProvider.class);
		defaultClasses.add(TargetFilterAnnotationInjectableProvider.class);

		defaultApplication = new ImmutableApplication(defaultClasses, null);
	}

	public synchronized void addSingletons(String alias, Set<Object> newResources) throws ServletException {
		ImmutableApplication newApp;
		ImmutableApplication existingApp = appMap.remove(alias);
		if (existingApp == null)
			existingApp = defaultApplication;
		newApp = existingApp.addSingletons(newResources);
		appMap.put(alias, newApp);
		replaceAlias(alias, newApp);
	}
	
	public synchronized void addClasses(String alias, Set<Class<?>> newClasses) throws ServletException {
		ImmutableApplication newApp;
		synchronized (appMap) {
		ImmutableApplication existingApp = appMap.remove(alias);
		if (existingApp == null)
			existingApp = defaultApplication;
		newApp = existingApp.addClasses(newClasses);
		appMap.put(alias, newApp);
		}
		replaceAlias(alias, newApp);
	}

	public synchronized void removeSingletons(String alias, final Collection<? extends Object> removedResources) throws ServletException {
		ImmutableApplication newApp;
		ImmutableApplication existingApp = appMap.remove(alias);
		if (existingApp != null) {
			newApp = existingApp.removeSingletons(removedResources);
			appMap.put(alias, newApp);
		} else {
			newApp = null;
		}
		if (defaultApplication.equals(newApp))
			newApp = null;
		replaceAlias(alias, newApp);
	}

	public synchronized void removeClasses(String alias, final Collection<Class<?>> removedClasses) throws ServletException {
		ImmutableApplication newApp;
		ImmutableApplication existingApp = appMap.remove(alias);
		if (existingApp != null) {
			newApp = existingApp.removeClasses(removedClasses);
			appMap.put(alias, newApp);
		} else {
			newApp = null;
		}
		if (defaultApplication.equals(newApp))
			newApp = null;
		replaceAlias(alias, newApp);
	}
	
	private synchronized void replaceAlias(String alias, Application application) throws ServletException {
		if (application == null) {
			servletMap.remove(alias);

			unregisterEndpointServices(alias);
			updateServlets();
		} else {
			ServletHolder servletHolder = new ServletHolder(UUID.randomUUID().toString(), new ServletContainer(application));
			servletMap.put(alias, servletHolder);

			updateServlets();
			registerEndpointServices(alias);
		}
	}

	private synchronized void updateServlets() {
		ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(RestAppServletManager.class.getClassLoader());
			
			List<ServletHolder> holders = new ArrayList<ServletHolder>(servletMap.values());
			List<ServletMapping> mappings = new ArrayList<ServletMapping>(holders.size());
			
			for (Entry<String, ServletHolder> entry : servletMap.entrySet()) {
				ServletMapping mapping = new ServletMapping();
				mapping.setPathSpec(entry.getKey() + "/*");
				mapping.setServletName(entry.getValue().getName());
				mappings.add(mapping);
			}
			servletHandler.setServlets(holders.toArray(new ServletHolder[holders.size()]));
			servletHandler.setServletMappings(mappings.toArray(new ServletMapping[mappings.size()]));
		} finally {
			Thread.currentThread().setContextClassLoader(oldLoader);
		}
	}
	
	private synchronized void registerEndpointServices(String alias) {
		List<String> expandedUris = new ArrayList<String>(endpointAddresses.size());
		
		for (URI uri : endpointAddresses) {
			try {
				 URI expandedUri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), alias, uri.getQuery(), uri.getFragment());
				 expandedUris.add(expandedUri.toString());
			} catch (URISyntaxException e) {
				// really, how the hell can this happen...
				throw new RuntimeException(e);
			}
		}
		
		Hashtable<String, Object> svcProps = new Hashtable<String, Object>(expandedUris.size());
		svcProps.put(Endpoint.URI, expandedUris.toArray(new String[expandedUris.size()]));
		svcProps.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES, WILDCARD);
		
		ServiceRegistration<?> reg = context.registerService(Endpoint.class.getName(), new Endpoint() {}, svcProps);
		ServiceRegistration<?> previous = endpointRegistrations.put(alias, reg);
		if (previous != null)
			previous.unregister();
	}

	private synchronized void unregisterEndpointServices(String alias) {
		ServiceRegistration<?> httpReg = endpointRegistrations.remove(alias);
		if (httpReg != null)
			httpReg.unregister();
	}
	
	public synchronized void destroyAll() {
		servletMap.clear();
		updateServlets();
		
		for (Iterator<Entry<String, ServiceRegistration<?>>> iter = endpointRegistrations.entrySet().iterator(); iter.hasNext(); ) {
			Entry<String, ServiceRegistration<?>> entry = iter.next();
			entry.getValue().unregister();
			iter.remove();
		}
	}

}
