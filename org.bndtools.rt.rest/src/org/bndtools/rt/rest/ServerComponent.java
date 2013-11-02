package org.bndtools.rt.rest;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.bndtools.rt.utils.log.LogTracker;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentContext;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

@Component(
		name = BundleConstants.EXTENDER_ID,
		designateFactory = ServerComponent.Config.class)
public class ServerComponent {
	
	private static final String HTTP_SCHEME = "http";

	private LogTracker logTracker;
	private Server server;
	private RestAppServletManager manager;
	private ResourceClassTracker classTracker;
	private ResourceServiceTracker serviceTracker;

	interface Config {
		@Meta.AD
		int port();
		
		@Meta.AD(required = false) // default null
		String host();
		
		@Meta.AD(required = false) // default null
		String filter();
	}

	@Activate
	void activate(ComponentContext context) throws Exception {
		BundleContext bc = context.getBundleContext();

		logTracker = new LogTracker(bc);
		logTracker.open();

		Config config = Configurable.createConfigurable(Config.class, context.getProperties());
		SelectChannelConnector connector = new SelectChannelConnector();
		connector.setPort(config.port());
		connector.setHost(config.host());
		
		Filter filter = config.filter() != null ? FrameworkUtil.createFilter(config.filter()) : null;
		
		server = new Server();
		server.setConnectors(new Connector[] { connector });
		
		ServletContextHandler servletContext = new ServletContextHandler();
		server.setHandler(servletContext);
		
		server.start();
		
		List<URI> endpointAddresses = getLocalAddresses(HTTP_SCHEME, config.host(), config.port(), false);
		manager = new RestAppServletManager(bc, servletContext.getServletHandler(), endpointAddresses);

		classTracker = new ResourceClassTracker(bc, manager, filter, logTracker);
		classTracker.open();
		
		serviceTracker = new ResourceServiceTracker(bc, manager, logTracker);
		serviceTracker.open();
	}
	
	void deactivate() throws Exception {
		serviceTracker.close();
		classTracker.close();
		manager.deleteAll();
		server.stop();
		logTracker.close();
	}
	
	List<URI> getLocalAddresses(String scheme, String requestedHost, int port, boolean enableIPv6) throws SocketException, URISyntaxException, UnknownHostException {
		List<URI> uris;
		if (requestedHost == null) {
			// Find all local interface addresses except loopback or multicast
			uris = new LinkedList<URI>();
			for (Enumeration<NetworkInterface> netInterEnum = NetworkInterface.getNetworkInterfaces(); netInterEnum.hasMoreElements(); ) {
				NetworkInterface netInter = netInterEnum.nextElement();
				for (Enumeration<InetAddress> addresses = netInter.getInetAddresses(); addresses.hasMoreElements(); ) {
					InetAddress address = addresses.nextElement();
					if (address.isLoopbackAddress() || address.isMulticastAddress())
						continue;
					if (!enableIPv6 && address instanceof Inet4Address)
						uris.add(new URI(scheme, null, address.getHostAddress(), port, null, null, null));
					else if (enableIPv6 && address instanceof Inet6Address)
						// see IETF RFC 2732, literal IPv6 address must be surrounded with [..]
						uris.add(new URI(scheme, null, "[" + address.getHostAddress() + "]", port, null, null, null));
				}
			}
		} else {
			InetAddress address = InetAddress.getByName(requestedHost);
			if (address instanceof Inet4Address)
				uris = Collections.singletonList(new URI(scheme, null, address.getHostAddress(), port, null, null, null));
			else if (address instanceof Inet6Address)
				uris = Collections.singletonList(new URI(scheme, null, "[" + address.getHostAddress() + "]", port, null, null, null));
			else
				uris = Collections.emptyList();
		}
		return uris;
	}
	
}
