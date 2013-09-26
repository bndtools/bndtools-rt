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

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
					// SINGLE VALUE
					Class<?> serviceClass = (Class<?>) type;
					return singleValueReference(serviceClass, httpContext);
				} else if (type instanceof ParameterizedType){
					// Maybe collection?
					ParameterizedType paramType = (ParameterizedType) type;
					Type[] typeArgs = paramType.getActualTypeArguments();
					if (typeArgs == null || typeArgs.length != 1)
						throw unsupportedType(type);
					Class<?> serviceClass = (Class<?>) typeArgs[0];
					Class<?> rawClass = (Class<?>) paramType.getRawType();
					
					@SuppressWarnings("rawtypes")
					CollectionFactory factory;
					
					if (Set.class.equals(rawClass))
						factory = new HashSetFactory<Object>();
					else if (List.class.equals(rawClass) || Collection.class.equals(rawClass))
						factory = new ArrayListFactory<Object>();
					else
						throw unsupportedType(type);
					
					@SuppressWarnings({ "rawtypes", "unchecked" })
					Collection reference = multiValueReference(serviceClass, httpContext, factory);
					return reference;
				} else {
					throw unsupportedType(type);
				}
			}
			
			RuntimeException unsupportedType(Type type) {
				return new IllegalArgumentException("Unsupported injectable type: " + type);
			}
			
			<S> Collection<S> multiValueReference(Class<S> serviceClass, HttpContext httpContext, CollectionFactory<S> factory) {
				try {
					// Get the service references
					Collection<ServiceReference<S>> references = bundleContext.getServiceReferences(serviceClass, filter);
					if (references == null || references.isEmpty())
						return missingMultipleService(serviceClass, factory);
					
					Collection<S> result = factory.create(references.size());
					
					// Get the services
					List<ServiceReference<S>> refsToRelease = new ArrayList<ServiceReference<S>>(references.size());
					for (ServiceReference<S> reference : references) {
						S service = bundleContext.getService(reference);
						if (service != null) {
							result.add(service);
							refsToRelease.add(reference);
						}
					}
					
					// Create the closeable to release the services
					addCloseable(httpContext, bundleContext, refsToRelease);
					
					return result;
				} catch (InvalidSyntaxException e) {
					throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
				}
			}
			
			Object singleValueReference(Class<?> serviceClass, HttpContext httpContext) {
				try {
					// Get the service reference
					Collection<?> references = bundleContext.getServiceReferences(serviceClass, filter);
					if (references == null || references.isEmpty())
						return missingSingularService(serviceClass);
					@SuppressWarnings("unchecked")
					ServiceReference<Object> serviceRef = (ServiceReference<Object>) references.iterator().next();

					// Get the underlying service
					Object service = bundleContext.getService(serviceRef);
					if (service == null)
						return missingSingularService(serviceClass);
					
					// Create a closeable to release the service
					addCloseable(httpContext, bundleContext, Collections.singletonList(serviceRef));
					
					return service;
				} catch (InvalidSyntaxException e) {
					throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
				}				
			}
			
			Object missingSingularService(Class<?> serviceClass) {
				if (optional)
					return null;
				Response response = Response.status(Status.SERVICE_UNAVAILABLE).entity(serviceClass.getName()).type(MediaType.TEXT_PLAIN).build();
				throw new WebApplicationException(response);
			}
			
			<S> Collection<S> missingMultipleService(Class<S> serviceClass, CollectionFactory<S> factory) {
				if (optional)
					return factory.create(0);
				Response response = Response.status(Status.SERVICE_UNAVAILABLE).entity(serviceClass.getName()).type(MediaType.TEXT_PLAIN).build();
				throw new WebApplicationException(response);
			}
		};
	}
	
	void addCloseable(HttpContext httpContext, final BundleContext bundleContext, final Collection<? extends ServiceReference<?>> serviceRefs) {
		@SuppressWarnings("unchecked")
		Set<Closeable> closeableSet = (Set<Closeable>) httpContext.getProperties().get(CloseableServiceFactory.class.getName());
		if (closeableSet == null) {
			closeableSet = new HashSet<Closeable>();
			httpContext.getProperties().put(CloseableServiceFactory.class.getName(), closeableSet);
		}
		closeableSet.add(new Closeable() {
			public void close() throws IOException {
				for (ServiceReference<?> serviceRef : serviceRefs) {
					bundleContext.ungetService(serviceRef);
				}
			}
		});
	}

	public ComponentScope getScope() {
		return ComponentScope.PerRequest;
	}

}
