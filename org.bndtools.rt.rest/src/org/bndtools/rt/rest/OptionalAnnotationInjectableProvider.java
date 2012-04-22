package org.bndtools.rt.rest;

import java.lang.reflect.Type;

import javax.ws.rs.ext.Provider;

import org.bndtools.inject.Optional;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

@Provider
public class OptionalAnnotationInjectableProvider extends BaseInjectableProvider implements InjectableProvider<Optional, Type> {

	public Injectable<Object> getInjectable(ComponentContext context, Optional annotation, Type type) {
		return super.getInjectable(context, type);
	}

}
