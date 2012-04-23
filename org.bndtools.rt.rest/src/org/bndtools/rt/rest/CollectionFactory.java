package org.bndtools.rt.rest;

import java.util.Collection;

public interface CollectionFactory {
	Collection<Object> create(int size);
}
