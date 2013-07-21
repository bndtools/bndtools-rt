package org.bndtools.rt.browserkit;

import java.util.Properties;

import org.eclipse.swt.widgets.Display;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class BrowserKitActivator implements BundleActivator {
	
	@Override
	public void start(final BundleContext context) throws Exception {
		Runnable runnable = new Runnable() {
			public void run() {
				Display display = new Display();
				
				EndpointTracker tracker = new EndpointTracker(context, display);
				System.out.println("Waiting for http(s) Endpoints");
				tracker.open();
				
				while (!display.isDisposed()) {
					if (!display.readAndDispatch())
						display.sleep();
				}
				tracker.close();
			}
		};
		
		Properties props = new Properties();
		props.put("main.thread", "true");
		context.registerService(Runnable.class.getName(), runnable, props);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

}
