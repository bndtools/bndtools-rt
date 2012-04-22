package org.bndtools.rt.rest;

import java.lang.reflect.Type;

import javax.ws.rs.ext.Provider;

import org.bndtools.inject.TargetFilter;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

@Provider
public class TargetFilterAnnotationInjectableProvider extends BaseInjectableProvider implements InjectableProvider<TargetFilter, Type> {

	public Injectable<Object> getInjectable(ComponentContext context, TargetFilter annotation, Type type) {
		return super.getInjectable(context, type);
	}

}
