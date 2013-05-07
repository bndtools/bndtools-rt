/*******************************************************************************
 * Copyright (c) 2012 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package org.bndtools.rt.packager.mosquitto.unix;

import java.io.File;
import java.util.Map;

import org.bndtools.service.mosquitto.MosquittoProperties;
import org.bndtools.service.packager.PackageDescriptor;
import org.bndtools.service.packager.PackageType;

import aQute.bnd.annotation.metatype.Configurable;
import aQute.lib.io.IO;
import aQute.libg.command.Command;

public abstract class MosquittoPackagerUNIX implements PackageType {

	@Override
	public PackageDescriptor create(Map<String, Object> properties, File data) throws Exception {
		MosquittoProperties config = Configurable.createConfigurable(MosquittoProperties.class, properties);
		
		PackageDescriptor pd = new PackageDescriptor();
		
		File mosquitto = new File(data, "mosquitto");
		installIfAbsent(mosquitto, true);
		installDynamicLibs(data);
		
		StringBuilder sb = new StringBuilder();
		appendDynamicLibConfig(sb, data);
		sb.append("exec ").append(mosquitto.getAbsolutePath());
		if (config.port() > 0)
			sb.append(" -p ").append(config.port());
		pd.startScript = sb.toString();
		
		pd.description = "Mosquitto: an MQTT v3.1 broker";
		
		return pd;
	}
	
	/**
	 * Install any dynamic-linked libraries required by the executable. This
	 * implementation does nothing; subclasses may override.
	 */
	protected void installDynamicLibs(File dataDir) throws Exception {
	}

	/**
	 * Append dynamic-linked library configuration to the start command, e.g.
	 * "export LD_LIBRARY_PATH" or "export DYLD_LIBRARY_PATH". This
	 * implementation does nothing; subclasses may override.
	 */
	protected void appendDynamicLibConfig(StringBuilder sb, File dataDir) {
	}

	protected void installIfAbsent(File file, boolean executable) throws Exception {
		if (!file.isFile()) {
			IO.copy(getClass().getResource("/data/" + file.getName()), file);
			if (executable)
				run("chmod a+x " + file.getAbsolutePath());
		}
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
