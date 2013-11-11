package org.bndtools.rt.rest.whiteboard.jetty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.servlet.Servlet;

import org.bndtools.rt.utils.classloader.BundleClassLoader;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.osgi.framework.Bundle;

import aQute.libg.tuple.Pair;

public final class ServletManager {

	private final Map<String, Pair<ServletHolder, Bundle>> servletMap = new LinkedHashMap<String, Pair<ServletHolder, Bundle>>();
	
	private final ServletHandler servletHandler;

	public ServletManager(ServletHandler servletHandler) {
		this.servletHandler = servletHandler;
	}
	
	public synchronized void putAlias(String alias, Servlet servlet, Bundle publisher) {
		ServletHolder holder = new ServletHolder(UUID.randomUUID().toString(), servlet);
		servletMap.put(alias, new Pair<ServletHolder, Bundle>(holder, publisher));
		updateServlets();
	}
	
	public synchronized void removeAlias(String alias) {
		Pair<ServletHolder,Bundle> mapped = servletMap.remove(alias);
		if (mapped != null)
			updateServlets();
	}
	
	private synchronized void updateServlets() {
		// Clear handlers and mappings
		servletHandler.setServlets(new ServletHolder[0]);
		servletHandler.setServletMappings(new ServletMapping[0]);
		
		// Add in each servlet with its mapping
		for (Entry<String, Pair<ServletHolder, Bundle>> entry : servletMap.entrySet()) {
			ServletHolder servlet = entry.getValue().getFirst();
			String pathSpec = entry.getKey();
			if (pathSpec.endsWith("/"))
				pathSpec += "*";
			else
				pathSpec += "/*";
			
			Bundle publisher = entry.getValue().getSecond();
			
			BundleClassLoader loader = new BundleClassLoader(publisher);
			ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(loader);
				servletHandler.addServletWithMapping(servlet, pathSpec);
			} finally {
				Thread.currentThread().setContextClassLoader(oldLoader);
			}
			// servletHandler.setServlets(holders.toArray(new ServletHolder[holders.size()]));
			// servletHandler.setServletMappings(mappings.toArray(new ServletMapping[mappings.size()]));
		}
	}
	
}
