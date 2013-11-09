package org.bndtools.rt.rest.whiteboard.jetty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.servlet.Servlet;

import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;

public final class ServletManager {

	private final Map<String, ServletHolder> servletMap = new LinkedHashMap<String, ServletHolder>();
	
	private final ServletHandler servletHandler;

	public ServletManager(ServletHandler servletHandler) {
		this.servletHandler = servletHandler;
	}
	
	public synchronized void putAlias(String alias, Servlet servlet) {
		ServletHolder holder = new ServletHolder(UUID.randomUUID().toString(), servlet);
		servletMap.put(alias, holder);
		updateServlets();
	}
	
	public synchronized void removeAlias(String alias) {
		ServletHolder mapped = servletMap.remove(alias);
		if (mapped != null)
			updateServlets();
	}
	
	private synchronized void updateServlets() {
		ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
			
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
	
}
