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
package org.example.tests.examples;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.example.tests.api.MyRunnable;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

@Component(provide = Object.class, properties = "osgi.rest.alias=/singleton2")
@Path("/foo")
public class SingletonServiceResource2 {

	private MyRunnable runnable;

	@Reference
	public void bind(MyRunnable runnable) {
		this.runnable = runnable;
	}
	
	@GET
	@Produces("text/html")
	public String getHtml() {
		runnable.run();
		return "<html><head></head><body>\n"
				+ "This is an easy resource (as html text).\n"
				+ "</body></html>";
	}

	@GET
	@Produces("text/plain")
	public String getPlain() {
		runnable.run();
		return "This is an easy resource (as plain text)";
	}
}
