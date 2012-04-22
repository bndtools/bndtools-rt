package org.bndtools.rt.rest;

import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

@Provider
public class InjectAnnotationInjectableProvider extends BaseInjectableProvider implements InjectableProvider<Inject, Type> {

	public Injectable<Object> getInjectable(ComponentContext context, Inject annotation, Type type) {
		return super.getInjectable(context, type);
	}

}
