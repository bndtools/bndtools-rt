package org.bndtools.rt.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.ws.rs.core.Application;

import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import com.sun.jersey.spi.container.servlet.ServletContainer;

public class RestAppServletManager {

	private final HttpService httpService;
	private final Map<String, ImmutableApplication> appMap = new HashMap<String, ImmutableApplication>();
	
	private final ImmutableApplication defaultApplication;
	
	public RestAppServletManager(HttpService httpService) {
		this.httpService = httpService;
		
		Set<Class<?>> defaultClasses = new HashSet<Class<?>>();
		defaultClasses.add(InjectAnnotationInjectableProvider.class);
		defaultClasses.add(OptionalAnnotationInjectableProvider.class);
		defaultApplication = new ImmutableApplication(defaultClasses, null);
	}

	public void addSingletons(String alias, Set<Object> newResources) throws ServletException, NamespaceException {
		final ImmutableApplication newApp;
		synchronized (appMap) {
			ImmutableApplication existingApp = appMap.remove(alias);
			if (existingApp == null)
				existingApp = defaultApplication;
			newApp = existingApp.addSingletons(newResources);
			appMap.put(alias, newApp);
		}
		replaceAlias(alias, newApp);
	}
	
	public void addClasses(String alias, Set<Class<?>> newClasses) throws ServletException, NamespaceException {
		final ImmutableApplication newApp;
		synchronized (appMap) {
			ImmutableApplication existingApp = appMap.remove(alias);
			if (existingApp == null)
				existingApp = defaultApplication;
			newApp = existingApp.addClasses(newClasses);
			appMap.put(alias, newApp);
		}
		replaceAlias(alias, newApp);
	}

	public void removeSingletons(String alias, final Collection<? extends Object> removedResources) throws ServletException, NamespaceException {
		ImmutableApplication newApp;
		synchronized (appMap) {
			ImmutableApplication existingApp = appMap.remove(alias);
			if (existingApp != null) {
				newApp = existingApp.removeSingletons(removedResources);
				appMap.put(alias, newApp);
			} else {
				newApp = null;
			}
		}
		if (defaultApplication.equals(newApp))
			newApp = null;
		replaceAlias(alias, newApp);
	}

	public void removeClasses(String alias, final Collection<Class<?>> removedClasses) throws ServletException, NamespaceException {
		ImmutableApplication newApp;
		synchronized (appMap) {
			ImmutableApplication existingApp = appMap.remove(alias);
			if (existingApp != null) {
				newApp = existingApp.removeClasses(removedClasses);
				appMap.put(alias, newApp);
			} else {
				newApp = null;
			}
		}
		if (defaultApplication.equals(newApp))
			newApp = null;
		replaceAlias(alias, newApp);
	}
	
	private void replaceAlias(String alias, Application application) throws ServletException, NamespaceException {
		// Unregister old servlet for this alias
		try {
			httpService.unregister(alias);
		} catch (IllegalArgumentException e) {
			// it wasn't registered
		}

		// Register the servlet
		if (application != null) {
			ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(RestAppServletManager.class.getClassLoader());
				ServletContainer servlet = new ServletContainer(application);
				httpService.registerServlet(alias, servlet, null, null);
			} finally {
				Thread.currentThread().setContextClassLoader(oldLoader);
			}
		}
	}

	public void destroyAll() {
		List<String> aliases;
		synchronized (appMap) {
			aliases = new ArrayList<String>(appMap.keySet());
			appMap.clear();
		}
		for (String alias : aliases) {
			try {
				httpService.unregister(alias);
			} catch (Exception e) {
				// ignore
			}
		}
	}

}
