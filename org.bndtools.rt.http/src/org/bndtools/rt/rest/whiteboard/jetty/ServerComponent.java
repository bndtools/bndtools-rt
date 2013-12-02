package org.bndtools.rt.rest.whiteboard.jetty;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Servlet;

import org.bndtools.rt.utils.log.LogTracker;
import org.bndtools.service.endpoint.Endpoint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;
import aQute.libg.tuple.Pair;

@Component(
		name = "bndtools.rt.http",
		designateFactory = ServerComponent.Config.class)
public class ServerComponent {
	
	private static final String HTTP_SCHEME = "http";

	static final String PROP_ALIAS = "bndtools.rt.http.alias";
	static final String PROP_FILTER = "filter";

	private LogTracker logTracker;
	private Server server;
	private ServletManager servletManager;
	private ServiceTracker<Servlet,Pair<String, ServiceRegistration<Endpoint>>> servletTracker;
	private EndpointPublisher publisher;

	static interface Config {
		@Meta.AD
		int port();
		
		@Meta.AD(required = false) // default null
		String host();
		
		@Meta.AD(required = false) // default null
		String filter();
		
		@Meta.AD(required = false)
		String[] mandatory();
	}

	@Activate
	void activate(ComponentContext context) throws Exception {
		final BundleContext bc = context.getBundleContext();

		logTracker = new LogTracker(bc);
		logTracker.open();

		@SuppressWarnings("unchecked")
		final Dictionary<String, ?> configProps = context.getProperties();

		Config config = Configurable.createConfigurable(Config.class, configProps);
		SelectChannelConnector connector = new SelectChannelConnector();
		connector.setPort(config.port());
		connector.setHost(config.host());
		
		server = new Server();
		server.setConnectors(new Connector[] { connector });
		
		ServletContextHandler servletContext = new ServletContextHandler();
		server.setHandler(servletContext);
		
		server.start();
		
		servletManager = new ServletManager(servletContext.getServletHandler());
		
		final List<URI> endpointAddresses = getLocalAddresses(HTTP_SCHEME, config.host(), config.port(), false);
		publisher = new EndpointPublisher(bc, endpointAddresses);
		
		final Set<String> mandatoryAttribs = new HashSet<String>();
		if (config.mandatory() != null)
			mandatoryAttribs.addAll(Arrays.asList(config.mandatory()));
		
		String serviceFilterStr = String.format("(&(%s=%s)(%s=*))", Constants.OBJECTCLASS, Servlet.class.getName(), PROP_ALIAS);
		servletTracker = new ServiceTracker<Servlet,Pair<String, ServiceRegistration<Endpoint>>>(bc, FrameworkUtil.createFilter(serviceFilterStr), null) {
			@Override
			public Pair<String, ServiceRegistration<Endpoint>> addingService(ServiceReference<Servlet> reference) {
				Servlet servlet = bc.getService(reference);
				Object aliasObj = reference.getProperty(PROP_ALIAS);
				Object filterObj = reference.getProperty(PROP_FILTER);

				Filter filter = null;
				String filterStr = null;
				if (filterObj != null && filterObj instanceof String) {
					filterStr = (String) filterObj;
					try {
						filter = FrameworkUtil.createFilter(filterStr);
					} catch (InvalidSyntaxException e) {
						logTracker.log(LogService.LOG_WARNING, "Invalid container filter on Servlet service", e);
					}
				}
				if (filter != null) {
					if (!filter.match(configProps)) {
						logTracker.log(LogService.LOG_INFO, "Excluding Servlet service based on container filter: " + filterStr);
						return null;
					}
				}
				
				if (mandatoryAttribs != null) for (String mandatoryAttrib : mandatoryAttribs) {
					if (filterStr == null || filterStr.indexOf(mandatoryAttrib) < 0) {
						logTracker.log(LogService.LOG_INFO, String.format("Excluding Servlet service: no match for mandatory attribute %s in filter: %s", mandatoryAttrib, filterStr));
						return null;
					}
				}

				if (aliasObj != null && aliasObj instanceof String) {
					String alias = (String) aliasObj;
					servletManager.putAlias(alias, servlet, reference.getBundle());
					
					Map<String, Object> endpointAttribs = ServiceProperties.copyProperties(reference);
					ServiceRegistration<Endpoint> svcReg = publisher.register(alias, endpointAttribs);
					
					return new Pair<String, ServiceRegistration<Endpoint>>(alias, svcReg);
				}
				return null;
			}
			@Override
			public void removedService(ServiceReference<Servlet> reference, Pair<String, ServiceRegistration<Endpoint>> registration) {
				registration.getSecond().unregister();
				servletManager.removeAlias(registration.getFirst());
				bc.ungetService(reference);
			}
		};
		servletTracker.open();
	}
	
	void deactivate() throws Exception {
		servletTracker.close();
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
