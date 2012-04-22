package org.bndtools.rt.rest;

import java.util.Collections;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

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
	
	public static ResourceServiceTracker newInstance(BundleContext context, RestAppServletManager manager, LogService log) {
		try {
			return new ResourceServiceTracker(context, manager, log);
		} catch (InvalidSyntaxException e) {
			// can't happen (?)
			throw new RuntimeException(e);
		}
	}

	private ResourceServiceTracker(BundleContext context, RestAppServletManager manager, LogService log) throws InvalidSyntaxException {
		super(context, FrameworkUtil.createFilter(FILTER_STRING), null);
		this.manager = manager;
		this.log = log;
	}
	
	@Override
	public Object addingService(@SuppressWarnings("rawtypes") ServiceReference reference) {
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
	public void removedService(@SuppressWarnings("rawtypes") ServiceReference reference, Object service) {
		context.ungetService(reference);
		
		RegisteredResource registered = (RegisteredResource) service;
		try {
			manager.removeSingletons(registered.alias, Collections.singleton(registered.resource));
		} catch (Exception e) {
			log.log(LogService.LOG_ERROR, String.format("Error removing resource from alias '%s'.", registered.alias), e);
		}
	}
}
