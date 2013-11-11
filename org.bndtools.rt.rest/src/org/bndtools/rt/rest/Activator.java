package org.bndtools.rt.rest;

import org.bndtools.rt.utils.log.LogTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private LogTracker logTracker;
	private ResourceServiceTracker resourceTracker;
	private ResourceClassTracker classTracker;

	@Override
	public void start(BundleContext context) throws Exception {
		logTracker = new LogTracker(context);
		logTracker.open();
		
		resourceTracker = new ResourceServiceTracker(context, logTracker);
		resourceTracker.open();
		
		classTracker = new ResourceClassTracker(context, logTracker);
		classTracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		classTracker.close();
		resourceTracker.close();
		logTracker.close();
	}

}
