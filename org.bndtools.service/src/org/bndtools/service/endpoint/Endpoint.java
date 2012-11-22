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
package org.bndtools.service.endpoint;

/**
 * This is a marker interface for advertising the existence of an endpoint
 * defined by a URI and optionally a version.
 * 
 * @author Neil Bartlett <njbartlett@gmail.com>
 * 
 */
public interface Endpoint {

	/**
	 * Service property "uri".
	 */
	static final String URI = "uri";

	/**
	 * Optional service property "version", which should be of type {@link org.osgi.framework.Version}
	 */
	static final String VERSION = "version";

}
