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
package org.bndtools.rt.packager.mongodb.guard;


import java.net.InetAddress;
import java.net.URI;
import java.util.Map;
import java.util.Properties;

import org.bndtools.service.endpoint.Endpoint;
import org.bndtools.service.mongodb.MongoProperties;
import org.bndtools.service.packager.ProcessGuard;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.metatype.Configurable;

@Component(
		name = MongoProcessGuard.PID, 
		designateFactory = MongoProperties.class,
		configurationPolicy = ConfigurationPolicy.optional,
		properties = {
			"type=mongodb",
			Constants.SERVICE_PID + "=" + MongoProcessGuard.PID
		})
public class MongoProcessGuard implements ProcessGuard {
	
	static final String PID = "org.bndtools.rt.packager.mongodb.guard";
	
	private static final String MONGO_URI_SCHEME = "mongodb";

	private BundleContext context;
	private Map<String, Object> props;
	private MongoProperties config;
	private ServiceRegistration reg;

	@Activate
	void activate(BundleContext context, Map<String, Object> props) throws Exception {
		this.context = context;
		this.props = props;
		this.config = Configurable.createConfigurable(MongoProperties.class, props);
	}

	public Map<String, Object> getProperties() {
		return props;
	}

	public synchronized void state(State state) throws Exception {
		if (state.isAlive()) {
			if (reg == null) {
				String localhost = InetAddress.getLocalHost().getHostAddress();
				URI mongoUri = new URI(MONGO_URI_SCHEME, null, localhost, config.port(), null, null, null);
				
				Properties svcProps = new Properties();
				svcProps.put(Endpoint.URI, mongoUri.toString());
				svcProps.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES, "*");
				reg = context.registerService(Endpoint.class.getName(), new Endpoint() {}, svcProps);
			}
		} else if (reg != null) {
			reg.unregister();
			reg = null;
		}
	}
}
