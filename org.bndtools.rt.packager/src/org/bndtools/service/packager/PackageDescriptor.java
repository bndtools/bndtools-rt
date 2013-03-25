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
 * Created by the Package Type from the Process Guard's configuration properties
 * in the {@link PackageType#create(java.util.Map, java.io.File)} method. It is
 * used by the {@link WatchDogManager} to construct the start, stop, and status
 * scripts.
 * <p/>
 * The scripts must be written for the default shell on the platform and
 * therefore platform specific. Scripts may consists of multiple lines. However,
 * the last line must ensure that the correct process status is result. This
 * usually requires an exec on Unix. Exec in this case replaces the shell
 * process with the command's process, ensuring that the result is from the last
 * command line and not the process. There are probably other ways as well.
 * <p/>
 * Scripts will all run with a working directory dedidcated to the process
 * instance. The same directory is always used for the given configuration
 * (based on the PID).
 * <p/>
 * Ensure that the creation of these scripts is done consistently. To check if
 * changes in the configuration affect the existing process, the
 * {@link WatchDogManager} will compare the scripts and other fields for any
 * changes. If they differ, existing processes are restarted.
 */
public class PackageDescriptor {

	/**
	 * Contains the start script to start the user process. For example:
	 * 
	 * <pre>
	 *  export SOME_CONF=dir
	 * 	exec mongo --port 5001 --db data
	 * </pre>
	 * 
	 * 
	 * This script is required.
	 */
	public String	startScript;

	/**
	 * Contains a script to stop the process gracefully. This is an optional
	 * script. If found, it is executed before the {@link WatchDogManager} will
	 * kill the process. The result of this script is ignored.
	 */
	public String	stopScript;

	/**
	 * If this script returns successfully it is assumed that the process is
	 * valid, alive, and happy. The {@link ProcessWatchDog} will regularly call
	 * this script to maintain the process' status so it can answer the ping
	 * requests.
	 */
	public String	statusScript;

	/**
	 * Provide a textual description.
	 */
	public String	description;
}
