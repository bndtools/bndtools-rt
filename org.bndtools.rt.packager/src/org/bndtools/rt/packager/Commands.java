package org.bndtools.rt.packager;

import java.util.List;
import java.util.Map.Entry;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;

@Component(provide = Object.class, properties = {
	"osgi.command.scope=pkgr",
	"osgi.command.function=nativeCaps"
})
public class Commands {
	
	private BundleContext	context;

	@Activate
	public void activate(BundleContext context) {
		this.context = context;
	}
	
	public void nativeCaps() {
		Bundle sysBundle = context.getBundle(0);
		BundleWiring sysBundleWiring = sysBundle.adapt(BundleWiring.class);
		List<BundleCapability> caps = sysBundleWiring.getCapabilities("osgi.native");
		
		for (BundleCapability cap : caps) {
			System.out.println(cap.getNamespace() + ":");
			for (Entry<String, Object> entry : cap.getAttributes().entrySet()) {
				System.out.printf("    %s=%s:%s%n", entry.getKey(), entry.getValue(), entry.getValue() != null ? entry.getValue().getClass().getName() : "null");
			}
			for (Entry<String, String> entry : cap.getDirectives().entrySet()) {
				System.out.printf("    %s:=%s%n", entry.getKey(), entry.getValue());
			}
		}
	}
}
