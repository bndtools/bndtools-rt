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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.Servlet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;

import com.sun.jersey.spi.container.servlet.ServletContainer;

public class ResourceClassTracker extends BundleTracker<ServiceRegistration<Servlet>> {
	
	public static final String REST_ALIAS = "REST-Alias";
	public static final String REST_CLASSES = "REST-Classes";
	public static final String REST_ENDPOINT_NAME = "REST-EndpointName";

	private static final String DEFAULT_ALIAS = "/";
	private static final String CLASS_LIST_SEPARATOR = ",";

	private final LogService log;
	
	public ResourceClassTracker(BundleContext context, LogService log) {
		super(context, Bundle.ACTIVE | Bundle.STARTING, null);
		this.log = log;
	}
	
	@Override
	public ServiceRegistration<Servlet> addingBundle(Bundle bundle, BundleEvent event) {
		// Get the class list
		String classListStr = bundle.getHeaders().get(REST_CLASSES);
		if (classListStr == null)
			return null;

		// Check if this bundle requires our capability
		String requires = bundle.getHeaders().get(Constants.REQUIRE_CAPABILITY);
		if (requires == null)
			return null;
		Parameters parsed = OSGiHeader.parseHeader(requires);
		Attrs extenderAttrs = parsed.get(ExtenderNamespace.EXTENDER_NAMESPACE);
		if (extenderAttrs == null)
			return null;
		String filterStr = extenderAttrs.get(Constants.FILTER_DIRECTIVE + ":");
		if (filterStr == null)
			return null;
		try {
			Filter filter = FrameworkUtil.createFilter(filterStr);
			if (!filter.matches(BundleConstants.CAPABILITIES))
				return null;
		} catch (InvalidSyntaxException e) {
			log.log(LogService.LOG_ERROR, String.format("Invalid extender filter in bundle %s: %s.", bundle.getSymbolicName(), filterStr), e);
		}
		
		// We have a match... the bundle is targeting our extender.
		
		// Get the alias and attribs
		String alias;
		Map<String, String> attribs;
		
		String aliasStr = bundle.getHeaders().get(REST_ALIAS);
		if (aliasStr == null) {
			alias = DEFAULT_ALIAS;
			attribs = Collections.emptyMap();
		} else {
			Parameters aliasParms = OSGiHeader.parseHeader(aliasStr);
			if (aliasParms.size() != 1) {
				log.log(LogService.LOG_ERROR, String.format("Invalid " + REST_ALIAS + " header in bundle %s: must specify exactly one alias.", bundle.getSymbolicName()));
				return null;
			}
			Entry<String, Attrs> entry = aliasParms.entrySet().iterator().next();
			alias = entry.getKey();
			attribs = entry.getValue();
		}

		// Load resource classes
		StringTokenizer tokenizer = new StringTokenizer(classListStr, CLASS_LIST_SEPARATOR);
		Set<Class<?>> resourceClasses = new HashSet<Class<?>>();
		while (tokenizer.hasMoreTokens()) {
			String className = tokenizer.nextToken().trim();
			try {
				if (className.length() > 0)
					resourceClasses.add(bundle.loadClass(className));
			} catch (ClassNotFoundException e) {
				log.log(LogService.LOG_ERROR, String.format("Failed to load REST class '%s' from bundle '%s'.", className, bundle.getSymbolicName()), e);
			}
		}

		// Register resource classes
		if (!resourceClasses.isEmpty()) {
			try {
				ImmutableApplication app = ImmutableApplication.empty().addClasses(resourceClasses);
				ServletContainer servlet = new ServletContainer(app);
				
				Dictionary<String, Object> properties = new Hashtable<String, Object>();
				properties.put("bndtools.rt.http.alias", alias);
				for (Entry<String, String> attrib : attribs.entrySet()) {
					properties.put(attrib.getKey(), attrib.getValue());
				}
				
				ServiceRegistration<Servlet> registration = context.registerService(Servlet.class, servlet, properties);
				return registration;
			} catch (Exception e) {
				log.log(LogService.LOG_ERROR, String.format("Error adding resource class(es) to alias '%s'.", alias), e);
			}
		}
		
		return null;
	}
	
	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, ServiceRegistration<Servlet> registration) {
		registration.unregister();
	}
}
