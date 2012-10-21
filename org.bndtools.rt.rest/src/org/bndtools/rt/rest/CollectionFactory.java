package org.bndtools.rt.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public interface CollectionFactory<T> {
	Collection<T> create(int size);
}

class HashSetFactory<T> implements CollectionFactory<T> {
	public Collection<T> create(int size) {
		return new HashSet<T>(size); 
	}
	
}

class ArrayListFactory<T> implements CollectionFactory<T> {
	public Collection<T> create(int size) {
		return new ArrayList<T>();
	}
}