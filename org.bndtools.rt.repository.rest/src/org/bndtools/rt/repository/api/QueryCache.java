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
package org.bndtools.rt.repository.api;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

import aQute.bnd.annotation.ProviderType;

@ProviderType
public interface QueryCache {

	UUID createQuery(Collection<? extends Requirement> requirements) throws Exception;
	Collection<? extends Requirement> getQuery(UUID queryId) throws Exception;
	Map<Requirement, Collection<Capability>> getSolution(UUID queryId, Repository repository) throws Exception;
	
}
