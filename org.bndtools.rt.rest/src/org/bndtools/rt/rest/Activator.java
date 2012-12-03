/*******************************************************************************
 * Copyright (c) 2012 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package org.bndtools.rt.rest;

import org.bndtools.rt.utils.log.LogTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	
	private LogTracker logTracker;
	private HttpServiceTracker httpTracker;

	public void start(BundleContext context) throws Exception {
		logTracker = new LogTracker(context);
		logTracker.open();
		
		httpTracker = new HttpServiceTracker(context, logTracker);
		httpTracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		httpTracker.close();
		logTracker.close();
	}
	
}
