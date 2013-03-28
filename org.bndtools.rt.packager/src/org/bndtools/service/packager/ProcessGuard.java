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

import java.util.*;

import aQute.bnd.annotation.ProviderType;

@ProviderType
public interface ProcessGuard {
	String	TYPE	= "type";
	String	VERSION	= "version";

	public enum State {
		WAITING_FOR_TYPE(false), STARTING(false), STARTED(true), PINGED(true), STOPPING(
				false), STOPPED(false), FAILED(false);

		boolean	alive;

		State(boolean alive) {
			this.alive = alive;
		}

		public boolean isAlive() {
			return alive;
		}
	}

	public Map<String, Object> getProperties();

	public void state(State state) throws Exception;
}
