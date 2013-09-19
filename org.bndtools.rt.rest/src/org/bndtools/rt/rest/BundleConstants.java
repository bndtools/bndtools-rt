package org.bndtools.rt.rest;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.namespace.extender.ExtenderNamespace;

final class BundleConstants {

	static final String EXTENDER_ID = "bndtools.rt.rest";
	static final Version VERSION = new Version("2.0.0");
	static final Map<String, Object> CAPABILITIES;
	static {
		CAPABILITIES = new HashMap<String, Object>();
		CAPABILITIES.put(ExtenderNamespace.EXTENDER_NAMESPACE, EXTENDER_ID);
		CAPABILITIES.put(Constants.VERSION_ATTRIBUTE, VERSION);
	}

}
