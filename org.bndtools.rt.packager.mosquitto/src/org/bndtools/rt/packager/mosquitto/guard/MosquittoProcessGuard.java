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
package org.bndtools.rt.packager.mosquitto.guard;

import java.net.InetAddress;
import java.net.URI;
import java.util.Map;
import java.util.Properties;

import org.bndtools.service.endpoint.Endpoint;
import org.bndtools.service.mosquitto.MosquittoProperties;
import org.bndtools.service.packager.ProcessGuard;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.metatype.Configurable;

@Component(
		name = MosquittoProcessGuard.NAME,
		designateFactory = MosquittoProperties.class,
		configurationPolicy = ConfigurationPolicy.optional,
		properties = "package.type=mosquitto")
public class MosquittoProcessGuard implements ProcessGuard {
	
	static final String NAME = "org.bndtools.rt.packager.mosquitto.guard";
	static final String MOSQUITTO_URI_SCHEME = "mqtt";
	
	private BundleContext context;
	private Map<String, Object> configProps;
	private MosquittoProperties config;

	private ServiceRegistration reg = null;
	
	@Activate
	void activate(BundleContext context, Map<String, Object> configProps) throws Exception {
		this.context = context;
		this.configProps = configProps;
		this.config = Configurable.createConfigurable(MosquittoProperties.class, configProps);
	}
	
	@Override
	public Map<String, Object> getProperties() {
		return configProps;
	}

	@Override
	public synchronized void state(State state) throws Exception {
		if (state.isAlive()) {
			if (reg == null) {
				String localhost = InetAddress.getLocalHost().getHostAddress();
				URI uri = new URI(MOSQUITTO_URI_SCHEME, null, localhost, config.port(), null, null, null);
				
				Properties svcProps = new Properties();
				svcProps.put(Endpoint.URI, uri.toString());
				svcProps.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES, "*");
				reg = context.registerService(Endpoint.class.getName(), new Endpoint() {}, svcProps);
			}
		} else if (reg != null) {
			reg.unregister();
			reg = null;
		}
	}

}
