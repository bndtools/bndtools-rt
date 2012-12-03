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
package org.bndtools.rt.packager.mongodb.unix;

import java.io.File;
import java.util.Map;

import org.bndtools.service.mongodb.MongoProperties;
import org.bndtools.service.packager.PackageDescriptor;
import org.bndtools.service.packager.PackageType;

import aQute.bnd.annotation.component.Component;
import aQute.lib.converter.Converter;
import aQute.lib.io.IO;
import aQute.libg.command.Command;


@Component(properties = "package.type=mongodb")
public class MongoPackagerUNIX implements PackageType {
	@Override
	public PackageDescriptor create(Map<String,Object> properties, File data) throws Exception {
		MongoProperties config = Converter.cnv(MongoProperties.class, properties);

		PackageDescriptor pd = new PackageDescriptor();
		File mongod = new File(data, "mongod");
		File mongo = new File(data, "mongo");
		if (!mongod.isFile()) {
			IO.copy(getClass().getResource("/data/mongod"), mongod);
			IO.copy(getClass().getResource("/data/mongo"), mongo);
			run("chmod a+x " + mongod.getAbsolutePath() + " " + mongo.getAbsolutePath());
		}

		StringBuilder sb = new StringBuilder().append(mongod.getAbsolutePath());
		if (config.port() != 0)
			sb.append(" --port ").append(config.port());
		if (config.quiet())
			sb.append(" --quiet");

		File db = new File(data, "db");
		if (config.dbpath() != null && !config.dbpath().isEmpty())
			db = IO.getFile(data, config.dbpath());
		sb.append(" --dbpath ").append(db.getAbsolutePath());
		db.mkdirs();

		pd.startScript = sb.toString();

		sb = new StringBuilder().append(mongo.getAbsolutePath());
		if (config.port() != 0)
			sb.append(" --port ").append(config.port());

		sb.append(" eval 'use admin; db.shutdownServer()'");
		pd.stopScript = sb.toString();

		sb = new StringBuilder().append(mongo.getAbsolutePath());
		if (config.port() != 0)
			sb.append(" --port ").append(config.port());

		sb.append(" --eval 'db.adminCommand({shutdown:1, force:true})'\n");
		sb.append("echo Wow");
		pd.stopScript = sb.toString();

		pd.statusScript = "echo Yes";

		pd.description = "MongoDB Packager";
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
