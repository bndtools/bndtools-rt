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
package org.bndtools.rt.utils.log;


import java.io.PrintStream;
import java.util.Date;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

public class LogTracker extends ServiceTracker implements LogService {
	
	public LogTracker(BundleContext context) {
		super(context, LogService.class.getName(), null);
	}
	
	public void log(int level, String message) {
		log(null, level, message, null);
	}

	public void log(int level, String message, Throwable exception) {
		log(null, level, message, exception);
	}

	public void log(@SuppressWarnings("rawtypes") ServiceReference sr, int level, String message) {
		log(sr, level, message, null);
	}

	public void log(@SuppressWarnings("rawtypes") ServiceReference sr, int level, String message, Throwable exception) {
		LogService log = (LogService) getService();
		
		if (log != null)
			log.log(sr, level, message, exception);
		else {
			PrintStream stream = (level <= LogService.LOG_WARNING) ? System.err : System.out;
			if (message == null)
				message = "";
			Date now = new Date();
			stream.println(String.format("[%-7s] %tF %tT: %s", LogUtils.formatLogLevel(level), now, now, message));
			if (exception != null)
				exception.printStackTrace(stream);
		}
	}


}
