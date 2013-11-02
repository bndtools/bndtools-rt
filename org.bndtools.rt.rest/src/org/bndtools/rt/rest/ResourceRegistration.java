package org.bndtools.rt.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.bndtools.service.endpoint.Endpoint;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import com.sun.jersey.spi.container.servlet.ServletContainer;

public class ResourceRegistration {

	private static final String WILDCARD = "*";

	private final String alias;
	private final BundleContext context;
	private final ServletManager servletMgr;
	private final List<URI> endpointAddresses;

	private final List<String> endpointNames = new ArrayList<String>();
	private ImmutableApplication application;

	private ServiceRegistration<Endpoint> svcReg;
	
	private ResourceRegistration(String alias, BundleContext context, ServletManager servletMgr, List<URI> endpointAddresses, ImmutableApplication application) {
		this.context = context;
		this.alias = alias;
		this.servletMgr = servletMgr;
		this.endpointAddresses = endpointAddresses;
		this.application = application;
	}

	public static ResourceRegistration create(String alias, BundleContext context, ServletManager servletMgr, List<URI> endpointAddresses) {
		ResourceRegistration registration = new ResourceRegistration(alias, context, servletMgr, endpointAddresses, new ImmutableApplication(null, null));
		registration.update();
		return registration;
	}
	
	public synchronized boolean isEmpty() {
		return application.isEmpty();
	}
	
	public synchronized void addClasses(Set<Class<?>> classes, String endpointName) {
		application = application.addClasses(classes);
		if (endpointName != null)
			endpointNames.add(endpointName);
		update();
	}
	
	public void removeClasses(Collection<Class<?>> classes, String endpointName) {
		application = application.removeClasses(classes);
		if (endpointName != null)
			endpointNames.remove(endpointName);
		update();
	}
	
	public void addSingletons(Set<Object> singletons, String endpointName) {
		application = application.addSingletons(singletons);
		if (endpointName != null)
			endpointNames.add(endpointName);
		update();
	}
	
	public void removeSingletons(Collection<? extends Object> singletons,  String endpointName) {
		application = application.removeSingletons(singletons);
		if (endpointName != null)
			endpointNames.remove(endpointName);
		update();
	}
	
	private synchronized void update() {
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
		svcProps.put("names", endpointNames.toArray(new String[endpointNames.size()]));
		
		if (application.isEmpty()) {
			// Remove existing service, if any
			if (svcReg != null) {
				svcReg.unregister();
				svcReg = null;
			}
			servletMgr.removeAlias(alias);
		} else {
			servletMgr.putAlias(alias, new ServletContainer(application));
			if (svcReg != null) {
				// RSA 1.0 does not support service update, so unregister and re-register instead.
				svcReg.unregister();
			}
			svcReg = context.registerService(Endpoint.class, new Endpoint() {}, svcProps);
		}
	}

	public synchronized void delete() {
		servletMgr.removeAlias(alias);
		if (svcReg != null)
			svcReg.unregister();
	}
	
}
