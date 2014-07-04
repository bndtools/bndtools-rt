package org.bndtools.rt.store.mongo;

import java.util.Map;

import org.bndtools.rt.store.mongo.MongoDBImpl.Config;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.lib.converter.Converter;

import com.mongodb.DB;
import com.mongodb.Mongo;

/**
 * This component is driven by a Managed Service Factory. It opens a Mongo DB,
 * gets a DB object and provides access to the stores. This component implements
 * the org.bndtools.service.store service.
 */
@Component(configurationPolicy = ConfigurationPolicy.require, designateFactory = Config.class)
public class MongoDBImpl implements org.bndtools.service.store.DB {
	Mongo		mongo;
	DB			db;
	LogService	log = null;

	public interface Config {
		/**
		 * The host name or null. If null, the mongo db should be on localhost
		 * and on the default port.
		 * 
		 * @return the host name
		 */
		String host();

		/**
		 * The port number, only used if not 0 and a host is set
		 * 
		 * @return the port number
		 */
		int port();

		/**
		 * The name of the db
		 * 
		 * @return the name of the db
		 */
		String db();

		/**
		 * The name of the db user. If set, a password must also be set.
		 */
		String user();

		/**
		 * The to be used password
		 */
		String _password(); // pl: why "_"

	};

	Config	config;

	/**
	 * Activate method
	 * @throws Exception 
	 */
	@Activate
	void activate(Map<String,Object> properties) throws Exception {
		config = Converter.cnv(Config.class, properties);

		// Get the host
		if (config.host() != null && config.host().length() > 1) {
			if (config.port() != 0)
				mongo = new Mongo(config.host(), config.port());
			else
				mongo = new Mongo(config.host());
		} else
			mongo = new Mongo();

		this.db = mongo.getDB(config.db());
		if (config.db().startsWith("test-")) {
			// all collections in databases that start with "test-" are always dropped 
			// for testing purposes.
			for (String name : db.getCollectionNames()) {
				if (!name.startsWith("system"))
					db.getCollection(name).drop();
			}
			//this.db.dropDatabase();
			//this.db = mongo.getDB(config.db()); // pl: why getting db again ?
		}

		// Log in if required
		if (config.user() != null && config.user().length() > 1 && config._password() != null) {
			db.authenticate(config.user(), config._password().toCharArray());
		}

	}

	/**
	 * Close the db and unregister the collections
	 */
	@Deactivate
	void deactivate() {
		mongo.close();
	}

	public <T> MongoStoreImpl<T> getStore(Class<T> clazz, String name) throws Exception {
		return new MongoStoreImpl<T>(this, clazz, db.getCollection(name));
	}

	@Override
	public void drop() {
		checkTest();
		for (String name : db.getCollectionNames()) {
			if (!name.startsWith("system"))
				db.getCollection(name).drop();
		}
	}

	@Reference
	public synchronized void setLogService(LogService log) {
		this.log = log;
	}

	void checkTest() {
		if (!config.db().startsWith("test-")) {
			String msg = "This is not a testing database (name must start with 'test-'), it is "
					+ config.db();
			if(log != null) { // pl: is this the right way of using an opt. service ?
				log.log(LogService.LOG_WARNING, msg);
			}
			throw new SecurityException(msg);
			
		}
			
		
	}
}
