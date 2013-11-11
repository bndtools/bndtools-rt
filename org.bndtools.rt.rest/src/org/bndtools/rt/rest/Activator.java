package org.bndtools.rt.rest;

import javax.servlet.Servlet;

import org.bndtools.rt.utils.log.LogTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private LogTracker logTracker;
	private ResourceServiceTracker resourceTracker;
	private AppRegistry appRegistry;

	@Override
	public void start(BundleContext context) throws Exception {
		logTracker = new LogTracker(context);
		logTracker.open();
		
		appRegistry = new AppRegistry();
		RegistrationManager<Servlet> regManager = new RegistrationManager<Servlet>(context, Servlet.class);
		
		resourceTracker = new ResourceServiceTracker(context, logTracker);
		resourceTracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		resourceTracker.close();
		logTracker.close();
	}

}
