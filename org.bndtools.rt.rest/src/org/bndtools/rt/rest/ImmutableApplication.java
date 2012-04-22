package org.bndtools.rt.rest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class ImmutableApplication extends Application {
	
	private final Set<Object> singletons;
	private final Set<Class<?>> classes;
	
	public ImmutableApplication(Set<Class<?>> classes, Set<Object> singletons) {
		this.classes = (classes != null) ? classes : Collections.<Class<?>>emptySet();
		this.singletons = (singletons != null) ? singletons : Collections.emptySet();
	}
	
	public ImmutableApplication addSingletons(Collection<? extends Object> toAdd) {
		Set<Class<?>> newClasses = new HashSet<Class<?>>(this.classes);
		
		Set<Object> newSingletons = new HashSet<Object>(this.singletons.size() + 1);
		newSingletons.addAll(this.singletons);
		newSingletons.addAll(toAdd);
		
		return new ImmutableApplication(newClasses, newSingletons);
	}
	
	public ImmutableApplication removeSingletons(Collection<? extends Object> toRemove) {
		Set<Class<?>> newClasses = new HashSet<Class<?>>(this.classes);
		
		Set<Object> newSingletons = new HashSet<Object>(this.singletons);
		newSingletons.removeAll(toRemove);
		
		return new ImmutableApplication(newClasses, newSingletons);
	}
	
	public ImmutableApplication addClasses(Collection<Class<?>> toAdd) {
		Set<Class<?>> newClasses = new HashSet<Class<?>>(this.classes.size() + 1);
		newClasses.addAll(this.classes);
		newClasses.addAll(toAdd);
		
		Set<Object> newSingletons = new HashSet<Object>(this.singletons);
		
		return new ImmutableApplication(newClasses, newSingletons);
	}
	
	public ImmutableApplication removeClasses(Collection<Class<?>> toRemove) {
		Set<Class<?>> newClasses = new HashSet<Class<?>>(this.classes);
		newClasses.removeAll(toRemove);
		
		Set<Object> newSingletons = new HashSet<Object>(this.singletons);
		
		return new ImmutableApplication(newClasses, newSingletons);
	}
	
	@Override
	public Set<Class<?>> getClasses() {
		return Collections.unmodifiableSet(classes);
	}
	
	@Override
	public Set<Object> getSingletons() {
		return Collections.unmodifiableSet(singletons);
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
