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

import java.io.File;
import java.util.Map;

import aQute.bnd.annotation.ProviderType;

/**
 * A Package Type service is responsible for creating scripts and other
 * configuration information from a configuration for a specific type of
 * process. 
 * 
 * TODO handle versioning
 */
@ProviderType
public interface PackageType {
	/**
	 * Service property identifying the process type TODO create namespace for
	 * this
	 */
	String	PACKAGE_TYPE	= "package.type";
	/**
	 * Service property identifying the process version
	 */
	String	VERSION			= "version";	

	/**
	 * Called by the Packager Manager to create scripts used to start, stop, and query
	 * an external process as managed by this Package Type.
	 * <p/>
	 * The given properties must at least contain:
	 * <ul>
	 * <li>
	 *  
	 * @param properties The prooper
	 * @param data
	 * @return
	 * @throws Exception
	 */
	PackageDescriptor create(Map<String, Object> properties, File data)
			throws Exception;
}
