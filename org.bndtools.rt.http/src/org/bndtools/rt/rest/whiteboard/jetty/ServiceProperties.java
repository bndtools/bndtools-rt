package org.bndtools.rt.rest.whiteboard.jetty;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;

public final class ServiceProperties {

	/**
	 * The set of property names that should not be copied from one service to
	 * another.
	 */
	public static final Set<String> NON_COPYING_PROPERTIES;

	static {
		Set<String> p = new HashSet<String>();

		p.add(Constants.OBJECTCLASS);
		p.add(Constants.SERVICE_ID);
		p.add(Constants.SERVICE_RANKING);
		p.add(ComponentConstants.COMPONENT_ID);
		p.add(ComponentConstants.COMPONENT_NAME);
		p.add(ServerComponent.PROP_ALIAS);
		p.add(ServerComponent.PROP_FILTER);

		NON_COPYING_PROPERTIES = Collections.unmodifiableSet(p);
	}

	private ServiceProperties() {
	}

	public static Map<String, Object> copyProperties(ServiceReference<?> ref) {
		Map<String, Object> result = new HashMap<String, Object>();

		String[] keys = ref.getPropertyKeys();
		for (String key : keys) {
			if (!NON_COPYING_PROPERTIES.contains(key))
				result.put(key, ref.getProperty(key));
		}

		return result;
	}

}
