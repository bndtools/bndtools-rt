package org.bndtools.rt.headlessJS.phantomJS.unix;

import java.io.File;
import java.util.Map;

import org.bndtools.rt.headlessJS.phantomJS.guard.PhantomSnapshotServerGuard.Config;
import org.bndtools.service.packager.PackageDescriptor;
import org.bndtools.service.packager.PackageType;
import org.bndtools.service.packager.ProcessGuard;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.lib.io.IO;
import aQute.libg.command.Command;

/**
 * {@link PackageType} encapsulating the headless javascript 
 * snapshot server for unix platforms. Configurations for this
 * server are those defined for the corresponding {@link ProcessGuard}
 * service.
 * 
 * This package type launches a phantomJS instance which runs the
 * {@code phantom/<platform>/snapshotServer.js} script (i.e. the 
 * actual headless interpreter server).
 */
@Component(properties = "package.type=phantomjs")
public class PhantomSnapshotServerTypeUNIX implements PackageType {
	
	int port;
	
	@Override
	public PackageDescriptor create(Map<String, Object> properties, File data)
			throws Exception {
		
		Config config = Configurable.createConfigurable(Config.class, properties);
		
		port = config.port() == 0 ? 8888 : config.port();
		
		PackageDescriptor pd = new PackageDescriptor();
		
		// START SCRIPT
		File phantom = new File(data, "phantomjs");
		if (!phantom.isFile()) {
			IO.copy(getClass().getResource("/data/phantomjs"), phantom);
			run("chmod a+x " + phantom.getAbsolutePath());
		}
		File script = new File(data, "snapshotServer.js");
		if (!script.isFile()) {
			IO.copy(getClass().getResource("/data/snapshotServer.js"), script);
		}
		
		
		StringBuilder sb = new StringBuilder().append(phantom.getAbsolutePath());
		sb.append(" ").append(script.getAbsolutePath());
		sb.append(" ").append(port);

		
		pd.startScript = sb.toString();
		
		/** 
		 * STATUS SCRIPT
		 * sends a "ping" command to the phantomJS server, expecting a "pong" response
		 */
		sb = new StringBuilder();
		sb.append("if [ \"$(curl -d \"command=ping\" 127.0.0.1:" + port + ")\" == \"pong\" ]; then").append("\n");
		sb.append("exit 0").append("\n");
		sb.append("else").append("\n");
		sb.append("exit -1").append("\n");
		sb.append("fi");
		pd.statusScript = sb.toString();
		
		/**
		 *  STOP SCRIPT
		 *  sends an "exit" command to the phantomJS server, expecting "exiting" in response 
		 */
		sb = new StringBuilder();
		sb.append("if [ \"$(curl -d \"command=exit\" 127.0.0.1:" + port + ")\" == \"exiting\" ]; then\n");
		sb.append("exit 0\n");
		sb.append("else\n");
		sb.append("exit -1\n");
		sb.append("fi");
		pd.stopScript = sb.toString();

		// DESCRIPTION
		pd.description = "PhantomJS packager";


		return pd;
	}

	private void run(String string) throws Exception {
		Command command = new Command("sh");
		StringBuilder out = new StringBuilder();
		StringBuilder err = new StringBuilder();
		int execute = command.execute(string, out, err);
		if (execute == 0)
			return;

		throw new Exception("command failed " + string + " : " + out + " : " + err);
	}
}
