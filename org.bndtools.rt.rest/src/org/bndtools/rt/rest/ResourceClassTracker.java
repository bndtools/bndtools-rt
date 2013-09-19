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

import java.util.HashSet;
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
	
	private final RestAppServletManager manager;
	private final LogService log;
	
	static final class RegisteredClass {
		final String alias;
		final Set<Class<?>> classes;
		RegisteredClass(String alias, Set<Class<?>> classes) {
			this.alias = alias;
			this.classes = classes;
		}
	}

	@SuppressWarnings("unchecked")
	public ResourceClassTracker(BundleContext context, RestAppServletManager manager, LogService log) {
		super(context, Bundle.ACTIVE | Bundle.STARTING, null);
		this.manager = manager;
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
		// we have a match!
		
		String alias = extenderAttrs.get("rest-alias");
		if (alias == null)
			alias = "";
		String classListStr = extenderAttrs.get("rest-classes");
		if (classListStr == null)
			return null;
		StringTokenizer tokenizer = new StringTokenizer(classListStr, ",");

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
				manager.addClasses(alias, resourceClasses);
				result = new RegisteredClass(alias, resourceClasses);
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
			manager.removeClasses(registered.alias, registered.classes);
		} catch (Exception e) {
			log.log(LogService.LOG_ERROR, String.format("Error removing resource class(es) from alias '%s'.", registered.alias), e);
		}
	}
}
