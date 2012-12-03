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
package org.bndtools.rt.packager.mongodb.client;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.service.command.Descriptor;
import org.bndtools.service.endpoint.Endpoint;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;

@Component(
		provide = Object.class,
		immediate = true,
		properties = {
				"osgi.command.scope=mongodb",
				"osgi.command.function=lsdb|use|lscoll"
		})
public class MongoCommands {
	
	private MongoURI boundUri;
	private Mongo mongo;
	private DB db;
	
	@Reference(target = "(uri=mongodb:*)")
	public void bindEndpoint(Endpoint ep, Map<String, String> props) throws Exception {
		boundUri = new MongoURI(props.get(Endpoint.URI));
	}
	
	@Activate
	public void activate() throws Exception {
		mongo = boundUri.connect();
		db = mongo.getDB("test");
		System.out.printf("Connected to MongoDB instance at address %s, database %s.%n", boundUri, db.getName());
	}
	
	@Deactivate
	public void deactivate() {
		mongo.close();
		System.out.println("Disconnected from " + boundUri);
	}

	@Descriptor("List databases")
	public void lsdb() {
		List<String> names = mongo.getDatabaseNames();
		System.out.printf("%d databases found: %s%n", names.size(), names);
	}
	
	@Descriptor("Switch to named database")
	public void use(@Descriptor("Database name") String dbname) {
		DB newDb = mongo.getDB(dbname);
		if (newDb != null) {
			db = newDb;
			System.out.printf("Switched to db %s%n", db.getName());
		}
	}
	
	@Descriptor("List collections")
	public void lscoll() {
		Set<String> names = db.getCollectionNames();
		System.out.printf("%d collections found: %s%n", names.size(), names);
	}

}
