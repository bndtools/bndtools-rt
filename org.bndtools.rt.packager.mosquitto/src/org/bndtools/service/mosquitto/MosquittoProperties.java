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
package org.bndtools.service.mosquitto;

import org.bndtools.service.packager.PackagerStandardProperties;

import aQute.bnd.annotation.metatype.Meta;



public interface MosquittoProperties extends PackagerStandardProperties {
	
	@Meta.AD(required = false, deflt = "1883")
	int port();
	
}
