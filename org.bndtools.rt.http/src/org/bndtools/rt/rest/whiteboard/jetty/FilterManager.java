package org.bndtools.rt.rest.whiteboard.jetty;

import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.Filter;

import org.bndtools.rt.utils.classloader.BundleClassLoader;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public final class FilterManager {

	private final SortedSet<FilterInfo> filters = 
			new TreeSet<FilterInfo>();
	
	private final ServletHandler servletHandler;

	public FilterManager(ServletHandler servletHandler) {
		this.servletHandler = servletHandler;
		servletHandler.setFilterChainsCached(false);
	}
	
	private final class FilterInfo implements Comparable<FilterInfo> {
		private final String pattern;
		private final FilterHolder holder;
		private final ServiceReference<Filter> ref;
		
		public FilterInfo(String pattern, FilterHolder holder, ServiceReference<Filter> ref) {
			this.pattern = pattern;
			this.holder = holder;
			this.ref = ref;
		}

		@Override
		public int compareTo(FilterInfo o) {
			return ref.compareTo(o.ref);
		}
	}
	
	public synchronized void addFilter(String pattern, Filter filter, ServiceReference<Filter> ref) {
		FilterHolder holder = new FilterHolder(filter);
		filters.add(new FilterInfo(pattern, holder, ref));
		updateFilters();
	}
	
	public synchronized void removeFilter(ServiceReference<Filter> ref) {
		if (filters.remove(new FilterInfo("", null, ref))) {
			updateFilters();
		}
	}
	
	private synchronized void updateFilters() {
		// Clear handlers and mappings
		servletHandler.setFilters(new FilterHolder[0]);
		servletHandler.setFilterMappings(new FilterMapping[0]);
		
		// Add in each servlet with its mapping
		for (FilterInfo fi : filters) {
			String pathSpec = fi.pattern;
			if (pathSpec.endsWith("/"))
				pathSpec += "*";
			else
				pathSpec += "/*";
			
			Bundle publisher = fi.ref.getBundle();
			
			BundleClassLoader loader = new BundleClassLoader(publisher);
			ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(loader);
				servletHandler.addFilterWithMapping(fi.holder, pathSpec, 1);
			} finally {
				Thread.currentThread().setContextClassLoader(oldLoader);
			}
		}
	}
	
}
