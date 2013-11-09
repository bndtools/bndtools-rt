package org.bndtools.rt.rest.whiteboard.jetty;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bndtools.service.endpoint.Endpoint;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class EndpointPublisher {

	private static final String WILDCARD = "*";

	private BundleContext context;
	private List<URI> addresses;

	public EndpointPublisher(BundleContext context, List<URI> addresses) {
		this.context = context;
		this.addresses = addresses;
	}
	
	public ServiceRegistration<Endpoint> register(String alias, Map<String, Object> attribs) {
		Dictionary<String, Object> svcProps = new Hashtable<String, Object>();
		svcProps.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES, WILDCARD);
		
		// Expand the URI list
		List<String> expandedUris = new ArrayList<String>(addresses.size());
		for (URI uri : addresses) {
			try {
				 URI expandedUri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), alias, uri.getQuery(), uri.getFragment());
				 expandedUris.add(expandedUri.toString());
			} catch (URISyntaxException e) {
				// really, how the hell can this happen...
				throw new RuntimeException(e);
			}
		}
		svcProps.put(Endpoint.URI, expandedUris.toArray(new String[expandedUris.size()]));
		
		// Add extra attribs
		for (Entry<String, Object> attrib : attribs.entrySet()) {
			svcProps.put(attrib.getKey(), attrib.getValue());
		}
		
		return context.registerService(Endpoint.class, new Endpoint() {}, svcProps);
	}
}
