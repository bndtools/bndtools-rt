package org.bndtools.rt.rest;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;

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

	public ResourceClassTracker(BundleContext context, RestAppServletManager manager, LogService log) {
		super(context, Bundle.ACTIVE | Bundle.STARTING, null);
		this.manager = manager;
		this.log = log;
	}
	
	@Override
	public Object addingBundle(Bundle bundle, BundleEvent event) {
		RegisteredClass result = null;
		
		// Read headers
		String alias = bundle.getHeaders().get("REST-Alias");
		if (alias == null || alias.length() == 0)
			alias = "/";
		String classListStr = bundle.getHeaders().get("REST-Classes");
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
