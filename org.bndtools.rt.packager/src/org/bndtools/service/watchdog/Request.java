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
package org.bndtools.service.watchdog;

public class Request {
	public enum Command {
		QUIT, PING, TRACE_ON, TRACE_OFF;
	};
	public Command request;
	public String id;
	
	/**
	 * Number of seconds for the process to stay alive
	 */
	public int next;
}
