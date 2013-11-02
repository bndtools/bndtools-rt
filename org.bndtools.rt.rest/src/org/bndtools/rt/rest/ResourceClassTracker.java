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
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;

@SuppressWarnings("rawtypes")
public class ResourceClassTracker extends BundleTracker {
	
	public static final String REST_ALIAS = "REST-Alias";
	public static final String REST_CLASSES = "REST-Classes";
	public static final String REST_ENDPOINT_NAME = "REST-EndpointName";

	private static final String DEFAULT_ALIAS = "";
	private static final String CLASS_LIST_SEPARATOR = ",";

	private final RestAppServletManager manager;
	private final LogService log;
	private final Filter attribFilter;
	
	static final class RegisteredClass {
		final String alias;
		final Set<Class<?>> classes;
		final String name;
		RegisteredClass(String alias, Set<Class<?>> classes, String name) {
			this.alias = alias;
			this.classes = classes;
			this.name = name;
		}
	}

	@SuppressWarnings("unchecked")
	public ResourceClassTracker(BundleContext context, RestAppServletManager manager, Filter filter, LogService log) {
		super(context, Bundle.ACTIVE | Bundle.STARTING, null);
		this.manager = manager;
		this.attribFilter = filter;
		this.log = log;
	}
	
	@Override
	public Object addingBundle(Bundle bundle, BundleEvent event) {
		RegisteredClass result = null;
		
		// Read header
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
		
		// Get endpoint name (default null)
		String endpointName = bundle.getHeaders().get(REST_ENDPOINT_NAME);
		if (endpointName != null) {
			endpointName = endpointName.trim();
			if (endpointName.length() == 0)
				endpointName = null;
		}
		
		// Get the class list
		String classListStr = bundle.getHeaders().get(REST_CLASSES);
		if (classListStr == null)
			return null;
		StringTokenizer tokenizer = new StringTokenizer(classListStr, CLASS_LIST_SEPARATOR);

		// Check the attribute filter specified in the config
		if (attribFilter != null) {
			boolean matches = attribFilter.matches(attribs);
			if (!matches) {
				log.log(LogService.LOG_INFO, String.format("Ignoring bundle %s due to non-matching attribute filter: %s.", bundle.getSymbolicName(), attribFilter));
				return null;
			}
		}
		
		// Load resource classes
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
				manager.addClasses(alias, resourceClasses, endpointName);
				result = new RegisteredClass(alias, resourceClasses, endpointName);
			} catch (Exception e) {
				log.log(LogService.LOG_ERROR, String.format("Error adding resource class(es) to alias '%s'.", alias), e);
			}
		}

		return result;
	}
	
	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
		RegisteredClass registered = (RegisteredClass) object;
		try {
			manager.removeClasses(registered.alias, registered.classes, registered.name);
		} catch (Exception e) {
			log.log(LogService.LOG_ERROR, String.format("Error removing resource class(es) from alias '%s'.", registered.alias), e);
		}
	}
}
