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
package org.bndtools.rt.rest.jaxrs.providers;

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
