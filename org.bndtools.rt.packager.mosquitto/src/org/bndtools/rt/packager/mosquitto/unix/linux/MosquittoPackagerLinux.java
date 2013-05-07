package org.bndtools.rt.packager.mosquitto.unix.linux;

import org.bndtools.rt.packager.mosquitto.unix.MosquittoPackagerUNIX;
import org.bndtools.service.packager.PackageType;

import aQute.bnd.annotation.component.Component;

@Component(properties = "package.type=mosquitto")
public class MosquittoPackagerLinux extends MosquittoPackagerUNIX implements PackageType {

	// no dynamic libs required
	
}
