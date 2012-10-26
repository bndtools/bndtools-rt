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
