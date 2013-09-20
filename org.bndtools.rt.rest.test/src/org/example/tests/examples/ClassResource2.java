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

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.example.tests.api.MyRunnable;

@Path("/foo2")
public class ClassResource2 {

	@Inject
	private MyRunnable runnable;

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
