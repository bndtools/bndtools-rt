package org.bndtools.rt.rest;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.bndtools.inject.Optional;
import org.bndtools.inject.TargetFilter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.application.CloseableServiceFactory;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;

public class BaseInjectableProvider {
	
	protected Injectable<Object> getInjectable(ComponentContext context, final Type type) {
		// Early exit if not annotations
		Annotation[] annotations = context.getAnnotations();
		if (annotations == null || annotations.length == 0)
			return null;
		
		// Put annotations into a set
		Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<Class<? extends Annotation>, Annotation>(annotations.length);
		for (Annotation annotation : annotations) {
			annotationMap.put(annotation.annotationType(), annotation);
		}
		
		// Look for the @Inject annotation & exit if not found
		if (!annotationMap.containsKey(Inject.class))
			return null;
		
		// Get some other settings from the annotations
		final boolean optional = annotationMap.containsKey(Optional.class);
		
		Annotation filterAnnot = annotationMap.get(TargetFilter.class);
		final String filter;
		if (filterAnnot == null) {
			filter = null;
		} else {
			filter = ((TargetFilter) filterAnnot).value(); 
		}
		
		System.out.printf("getInjectable for type %s, optional=%s", type, optional);
		
		// Find the owning bundle
		Class<?> resourceClass = null;
		AccessibleObject annotatedObj = context.getAccesibleObject();
		if (annotatedObj instanceof Member)
			resourceClass = ((Member) annotatedObj).getDeclaringClass();
		else
			throw new IllegalStateException("Annotation is not on a class member.");
		final Bundle bundle = FrameworkUtil.getBundle(resourceClass);
		if (bundle == null)
			throw new IllegalStateException("Resource class not loaded from an OSGi bundle: " + resourceClass);
		final BundleContext bundleContext = bundle.getBundleContext();
		
		// Create the Injectable
		return new AbstractHttpContextInjectable<Object>() {
			public Object getValue(HttpContext httpContext) {
				if (type instanceof Class) {
					try {
						// Get the service reference
						@SuppressWarnings("unchecked")
						Class<Object> serviceClass = (Class<Object>) type;
						Collection<ServiceReference<Object>> references = bundleContext.getServiceReferences(serviceClass, filter);
						if (references == null || references.isEmpty())
							return missingService();
						ServiceReference<Object> serviceRef = references.iterator().next();

						// Get the underlying service
						Object service = bundleContext.getService(serviceRef);
						if (service == null)
							return missingService();
						
						// Create a closeable to release the service
						addCloseable(httpContext, bundleContext, serviceRef);
						
						return service;
					} catch (InvalidSyntaxException e) {
						throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
					}
				} else {
					throw new IllegalArgumentException("Unsupported injectable type: " + type);
				}
			}
			
			Object missingService() {
				if (optional)
					return null;
				throw new WebApplicationException(Status.SERVICE_UNAVAILABLE);
			}
		};
	}
	
	void addCloseable(HttpContext httpContext, final BundleContext bundleContext, final ServiceReference<?> serviceRef) {
		@SuppressWarnings("unchecked")
		Set<Closeable> closeableSet = (Set<Closeable>) httpContext.getProperties().get(CloseableServiceFactory.class.getName());
		if (closeableSet == null) {
			closeableSet = new HashSet<Closeable>();
			httpContext.getProperties().put(CloseableServiceFactory.class.getName(), closeableSet);
		}
		closeableSet.add(new Closeable() {
			public void close() throws IOException {
				bundleContext.ungetService(serviceRef);
			}
		});
	}

	public ComponentScope getScope() {
		return ComponentScope.PerRequest;
	}

}
