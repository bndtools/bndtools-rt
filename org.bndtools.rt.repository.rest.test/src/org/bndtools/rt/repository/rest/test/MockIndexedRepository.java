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
package org.bndtools.rt.repository.rest.test;

import java.net.URI;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

import aQute.bnd.service.IndexProvider;
import aQute.bnd.service.ResolutionPhase;

public class MockIndexedRepository implements Repository, IndexProvider {
	
	private List<URI> indexes;
	private Map<Requirement, Collection<Capability>> result;
	
	public void setResult(Map<Requirement, Collection<Capability>> result) {
		this.result = result;
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		return result;
	}
	
	public void setIndexes(List<URI> indexes) {
		this.indexes = indexes;
	}

	@Override
	public List<URI> getIndexLocations() throws Exception {
		return indexes;
	}

	@Override
	public Set<ResolutionPhase> getSupportedPhases() {
		return EnumSet.allOf(ResolutionPhase.class);
	}

}
