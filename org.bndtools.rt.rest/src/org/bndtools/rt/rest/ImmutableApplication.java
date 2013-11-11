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
package org.bndtools.rt.rest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.bndtools.rt.rest.jaxrs.providers.InjectAnnotationInjectableProvider;
import org.bndtools.rt.rest.jaxrs.providers.OptionalAnnotationInjectableProvider;
import org.bndtools.rt.rest.jaxrs.providers.TargetFilterAnnotationInjectableProvider;

public class ImmutableApplication extends Application {
	
	private final Set<Class<?>> defaultClasses;
	private final Set<Class<?>> classes;
	private final Set<Class<?>> combinedClasses;
	
	private final Map<Object, Object> singletons;
	
	public static ImmutableApplication empty() {
		return new ImmutableApplication(null, null);
	}
	
	private ImmutableApplication(Set<Class<?>> classes, Map<Object, Object> singletons) {
		this.defaultClasses = initDefaultClasses();
		this.classes = (classes != null) ? classes : Collections.<Class<?>>emptySet();
		
		this.combinedClasses = new HashSet<Class<?>>(this.defaultClasses.size() + this.classes.size());
		this.combinedClasses.addAll(this.defaultClasses);
		this.combinedClasses.addAll(this.classes);
		
		this.singletons = (singletons != null) ? singletons : Collections.emptyMap();
	}

	private Set<Class<?>> initDefaultClasses() {
		Set<Class<?>> cs = new HashSet<Class<?>>();
		cs.add(InjectAnnotationInjectableProvider.class);
		cs.add(OptionalAnnotationInjectableProvider.class);
		cs.add(TargetFilterAnnotationInjectableProvider.class);
		return cs;
	}

	public ImmutableApplication addSingletons(Collection<? extends Object> toAdd) {
		Set<Class<?>> newClasses = new HashSet<Class<?>>(this.classes);
		
		Map<Object, Object> newSingletons = new IdentityHashMap<Object, Object>(this.singletons.size() + 1);
		newSingletons.putAll(this.singletons);
		for (Object object : toAdd)
			newSingletons.put(object, null);
		
		return new ImmutableApplication(newClasses, newSingletons);
	}
	
	public ImmutableApplication removeSingletons(Collection<? extends Object> toRemove) {
		Set<Class<?>> newClasses = new HashSet<Class<?>>(this.classes);
		
		Map<Object, Object> newSingletons = new IdentityHashMap<Object, Object>(this.singletons);
		for (Object object : toRemove)
			newSingletons.remove(object);
		
		return new ImmutableApplication(newClasses, newSingletons);
	}
	
	public ImmutableApplication addClasses(Collection<Class<?>> toAdd) {
		Set<Class<?>> newClasses = new HashSet<Class<?>>(this.classes.size() + 1);
		newClasses.addAll(this.classes);
		newClasses.addAll(toAdd);
		
		Map<Object, Object> newSingletons = new IdentityHashMap<Object, Object>(this.singletons);
		
		return new ImmutableApplication(newClasses, newSingletons);
	}
	
	public ImmutableApplication removeClasses(Collection<Class<?>> toRemove) {
		Set<Class<?>> newClasses = new HashSet<Class<?>>(this.classes);
		newClasses.removeAll(toRemove);
		
		Map<Object, Object> newSingletons = new IdentityHashMap<Object, Object>(this.singletons);
		
		return new ImmutableApplication(newClasses, newSingletons);
	}
	
	@Override
	public Set<Class<?>> getClasses() {
		return Collections.unmodifiableSet(combinedClasses);
	}
	
	@Override
	public Set<Object> getSingletons() {
		return Collections.unmodifiableSet(singletons.keySet());
	}
	
	public boolean isEmpty() {
		return classes.isEmpty() && singletons.isEmpty();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((classes == null) ? 0 : classes.hashCode());
		result = prime * result
				+ ((singletons == null) ? 0 : singletons.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImmutableApplication other = (ImmutableApplication) obj;
		if (classes == null) {
			if (other.classes != null)
				return false;
		} else if (!classes.equals(other.classes))
			return false;
		if (singletons == null) {
			if (other.singletons != null)
				return false;
		} else if (!singletons.equals(other.singletons))
			return false;
		return true;
	}

}
