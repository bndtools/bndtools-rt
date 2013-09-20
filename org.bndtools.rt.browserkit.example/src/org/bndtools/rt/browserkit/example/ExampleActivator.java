package org.bndtools.rt.browserkit.example;

import java.util.Properties;

import org.bndtools.service.endpoint.Endpoint;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ExampleActivator implements BundleActivator{

	public void start(BundleContext context) throws Exception {
		Properties props = new Properties();
		props.put(Endpoint.URI, "http://browser-info.com/");
		props.put("browserkit.title", "Example BrowserKit Application");
		
		context.registerService(Endpoint.class.getName(), new Endpoint() {}, props);
	}

	public void stop(BundleContext context) throws Exception {
	}

}
