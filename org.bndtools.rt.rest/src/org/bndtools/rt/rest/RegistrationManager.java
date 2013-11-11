package org.bndtools.rt.rest;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class RegistrationManager<S> {
	
	private final BundleContext context;
	private final Class<S> serviceClass;
	
	private final Map<String, ServiceRegistration<S>> registrations = new HashMap<String, ServiceRegistration<S>>();

	public RegistrationManager(BundleContext context, Class<S> serviceClass) {
		this.context = context;
		this.serviceClass = serviceClass;
	}
	
	public synchronized void register(String key, S service, Dictionary<String, ?> properties) {
		unregister(key);
		
		ServiceRegistration<S> registration = context.registerService(serviceClass, service, properties);
		registrations.put(key, registration);
	}
	
	public synchronized void unregister(String key) {
		ServiceRegistration<S> existing = registrations.remove(key);
		
		if(existing != null)
			existing.unregister();
	}

}
