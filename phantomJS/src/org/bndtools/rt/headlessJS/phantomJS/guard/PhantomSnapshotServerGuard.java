package org.bndtools.rt.headlessJS.phantomJS.guard;

import java.util.Map;

import org.bndtools.rt.headlessJS.phantomJS.guard.PhantomSnapshotServerGuard.Config;
import org.bndtools.service.packager.PackagerStandardProperties;
import org.bndtools.service.packager.ProcessGuard;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;


/**
 *  {@link ProcessGuard} managing an instance of a headless javascript
 *  snapshot server running on top of a phantomJS webserver. This service
 *  must be configured with the port number (defaulting to 8888) behind
 *  which the phantomJS webserver is running.
 *
 */
@Component(
		provide = ProcessGuard.class, 
		designateFactory = Config.class, 
		configurationPolicy = ConfigurationPolicy.optional, 
		properties = {
			"port=8888",
			"type=phantomjs",
			"service.pid=phantomJS"
			})
public class PhantomSnapshotServerGuard implements ProcessGuard {

	public interface Config extends PackagerStandardProperties {
		int port();
	}
	
	private Map<String, Object> properties;
	

	@Activate
	public void activate(Map<String, Object> properties) {
		this.properties = properties;
	}

	@Override
	public Map<String, Object> getProperties() {
		return properties;
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public void state(State state) throws Exception {
		switch(state) {
		case WAITING_FOR_TYPE:
			System.out.println("Phantom snapshot server guard: waiting for corresponding type");
			break;
		case STARTING:
			System.out.println("Phantom snapshot server guard: starting snapshot server");
			break;
		case STARTED:
			System.out.println("Phantom snapshot server guard: phantom snapshot server has started");
			break;
		case STOPPING:
			System.out.println("Phantom snapshot server guard: phantom snapshot server is stopping");
			break;
		case STOPPED:
			System.out.println("Phantom snapshot server guard: phantom snapshot server has stopped");
			break;
		case FAILED:
			System.err.println("Phantom snapshot server guard: phantom snapshot server has failed");
			break;
		}
	}

}
