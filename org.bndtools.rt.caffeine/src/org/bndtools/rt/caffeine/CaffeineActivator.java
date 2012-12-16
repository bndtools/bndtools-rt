package org.bndtools.rt.caffeine;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.BundleTracker;

public class CaffeineActivator implements BundleActivator {

	private BundleTracker tracker;

	@Override
	public void start(BundleContext context) throws Exception {
		tracker = new BundleTracker(context, Bundle.STARTING, null) {
			public Object addingBundle(Bundle bundle, BundleEvent event) {
				if (Constants.ACTIVATION_LAZY.equals(bundle.getHeaders().get(Constants.BUNDLE_ACTIVATIONPOLICY))) {
					try {
						bundle.start();
					} catch (BundleException e) {
						e.printStackTrace();
					}
					return bundle;
				}
				return null;
			}
		};
		tracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		tracker.close();
	}

}
