package org.bndtools.rt.packager.mosquitto.unix.macos;

import java.io.File;

import org.bndtools.rt.packager.mosquitto.unix.MosquittoPackagerUNIX;
import org.bndtools.service.packager.PackageType;

import aQute.bnd.annotation.component.Component;

@Component(properties = "package.type=mosquitto")
public class MosquittoPackagerMacOS extends MosquittoPackagerUNIX implements PackageType {

	@Override
	protected void installDynamicLibs(File dataDir) throws Exception {
		installIfAbsent(new File(dataDir, "libcrypto.1.0.0.dylib"), false);
		installIfAbsent(new File(dataDir, "libssl.1.0.0.dylib"), false);
	}
	
	@Override
	protected void appendDynamicLibConfig(StringBuilder builder, File dataDir) {
		builder.append("export DYLD_LIBRARY_PATH=\"").append(dataDir.getAbsolutePath()).append("\"\n");
	}
	
}
