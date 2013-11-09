package org.bndtools.rt.rest.whiteboard.jetty;

import org.bndtools.service.endpoint.Endpoint;
import org.osgi.framework.ServiceRegistration;

import aQute.libg.tuple.Pair;

class RegisteredServlet extends Pair<String, ServiceRegistration<Endpoint>> {

	RegisteredServlet(String alias, ServiceRegistration<Endpoint> serviceReg) {
		super(alias, serviceReg);
	}
	
}
