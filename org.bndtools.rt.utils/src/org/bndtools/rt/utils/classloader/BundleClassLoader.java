package org.bndtools.rt.utils.classloader;

import org.osgi.framework.Bundle;

public class BundleClassLoader extends ClassLoader {
	
	private final Bundle bundle;

	public BundleClassLoader(Bundle bundle) {
		this.bundle = bundle;
	}
	
	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class<?> clazz = bundle.loadClass(name);
		resolveClass(clazz);
		return clazz;
	}
}
