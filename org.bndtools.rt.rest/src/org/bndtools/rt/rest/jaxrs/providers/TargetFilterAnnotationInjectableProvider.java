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
