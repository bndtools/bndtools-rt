/*******************************************************************************
 * Copyright (c) 2012 Paremus Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Paremus Ltd - initial API and implementation
 ******************************************************************************/
package org.bndtools.service.packager;

/**
 * Standard properties to be used by configurations of Package Types. The
 * standard properties are used to configure the process management.
 */
public interface PackagerStandardProperties {
	/**
	 * Enum to enumerate the process nice levels.
	 */
	enum Priority {
		LOW, MEDIUM, HIGH
	};

	/**
	 * User id and optional domain according to platform standards.
	 * @return the user
	 */
	String user();

	/**
	 * Number of seconds between process pings.
	 * @return
	 */
	int ping();

	/**
	 * Priority of this process, a la nice.
	 * @return
	 */
	Priority priority();

	/**
	 * The Package Type
	 * @return
	 */
	String packageType();
}
