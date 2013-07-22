package org.bndtools.rt.browserkit.example;

import java.util.Properties;

import org.bndtools.rt.browserkit.api.BrowserKitConstants;
import org.bndtools.service.endpoint.Endpoint;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ExampleActivator implements BundleActivator{

	public void start(BundleContext context) throws Exception {
		Properties props = new Properties();
		props.put(Endpoint.URI, "https://google.com/");
		props.put(BrowserKitConstants.WINDOW_TITLE, "Example BrowserKit Application");
		
		context.registerService(Endpoint.class.getName(), new Endpoint() {}, props);
	}

	public void stop(BundleContext context) throws Exception {
	}

}
