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

import org.bndtools.inject.Optional;
import org.example.tests.api.MyRunnable;

// Exactly the same as ClassResource3, but the @Optional and @Inject annotations are the other way around
@Path("/foo4")
public class ClassResource4 {

	@Inject
	@Optional
	private MyRunnable runnable;

	@GET
	@Produces("text/plain")
	public String getPlain() {
		String response = (runnable == null) ? "NULL" : "NOT NULL";
		return response;
		
	}

}
