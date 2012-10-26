package org.bndtools.rt.repository.rest.test;

import java.util.Collection;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

public class MockRepository implements Repository {
	
	private Map<Requirement, Collection<Capability>> result;
	
	public void setResult(Map<Requirement, Collection<Capability>> result) {
		this.result = result;
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		return result;
	}

}
