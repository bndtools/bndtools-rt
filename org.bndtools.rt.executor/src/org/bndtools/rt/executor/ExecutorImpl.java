package org.bndtools.rt.executor;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.bndtools.rt.executor.ExecutorImpl.Config;
import org.bndtools.rt.executor.ExecutorImpl.Config.Type;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;

import static java.lang.Math.*;

/**
 * This bundle provides a java.util.concurrent.Executor service that can
 * configured (for multiple instances) and is shared between all bundles.
 * 
 * @see <a
 *      href="http://docs.oracle.com/javase/6/docs/api/java/util/concurrent/Executor.html">java.util.concurrent.Executor</a>
 */
@Component(designateFactory = Config.class, configurationPolicy = ConfigurationPolicy.require, servicefactory = true)
public class ExecutorImpl implements Executor {

	/**
	 * Configuration parameters expected from the Config Admin
	 */
	interface Config {
		enum Type {
			FIXED, CACHED, SINGLE
		}

		int service_ranking();
		int service_pid();
		int service_factoryPid();

		Type type();

		String id();

		int size();
	}

	/**
	 * Internal representation of the underlying (shared) instances of the
	 * thread pool, associated with the number of bundles using them
	 */
	static class EsHolder {
		ExecutorService	staticEs;
		int				counter;

		
		// pk: why not pass the config object? do the createConfigurable in activate? Is cleaner I think
 		
		EsHolder(Map<String,Object> properties) {
			counter = 0;
			Config config = Configurable.createConfigurable(Config.class, properties);
			Type t = config.type();
			if (t == null)
				t = Config.Type.FIXED;
			switch (t) {
				case FIXED :
					staticEs = Executors.newFixedThreadPool(max(config.size(), 2));
					break;
				case CACHED :
					staticEs = Executors.newCachedThreadPool();
					break;
				case SINGLE :
					staticEs = Executors.newSingleThreadExecutor();
					break;
			}
		}

		ExecutorService getEs() {
			counter++;
			return staticEs;
		}

		void unget() {
			counter--;
			if (counter == 0) {
				staticEs.shutdownNow();
			}
		}
	}

	static Map<String,EsHolder>	holders	= new HashMap<String,ExecutorImpl.EsHolder>();

	List<Future< ? >>			futures	= new ArrayList<Future< ? >>();				// List
																						// of
																						// tasks
																						// submitted
																						// by
																						// one
																						// bundle
	ExecutorService				es;													// Executor
																						// implementation
																						// used
																						// the
																						// bundle

	/**
	 * Creates a new instance of the underlying implementation of the executor
	 * service (depending on the configuration parameters) if needed, or returns
	 * a pre-existing instance of this service, shared by all bundles.
	 * 
	 * @param properties
	 *            Configuration parameters, passed by the framework
	 */
	@Activate
	void activate(Map<String,Object> properties) {

		// pk: you could use es = ConcurrentHashMap<EsHolder>.getIfAbsent( pid, new EsHolder(config))
		// does not need synchronized then
		
		synchronized (holders) {
			// pk: user config object ...?
			String pid = (String) properties.get("service.pid");
			if (!holders.containsKey(pid)) {
				holders.put(pid, new EsHolder(properties));
			}
			es = holders.get(pid).getEs();
		}

	}

	/**
	 * Cancels the tasks submitted by the exiting bundle, shutting down the
	 * executor service if no more bundle is using it
	 * 
	 * @param properties
	 *            Configuration parameters, passed by the framework
	 */
	@Deactivate
	void deactivate(Map<String,Object> properties) {

		
		synchronized (futures) {
			for (Future< ? > f : futures) {
				f.cancel(true);
			}
		}

		synchronized (holders) {
			// pk: the pid is used several times, I would make it a field
			String pid = (String) properties.get("service.pid");
			holders.get(pid).unget();
			if (holders.get(pid).counter == 0) {
				holders.remove(pid);
			}
		}

	}

	@Override
	public void execute(Runnable command) {
		Wrapper w = new Wrapper(command);
		Future< ? > f = es.submit(w);
		w.setFuture(f);
	}

	class Wrapper implements Runnable {

		// pk: fields with one name are not very instructive
		// better to use descriptive names and leave the single
		// character names for loops, or local vars where their
		// declaration is in close sight.
		
		Runnable		r;
		Future< ? >		f		= null;
		
		// pk: can use normal boolean
		AtomicBoolean	done	= new AtomicBoolean();

		Wrapper( Runnable command) {
			
			// pk: why yet another inner class? Just let Wrapper implement Runnable?
			
			r = command;
		}

		void setFuture(Future< ? > f) {
			this.f = f;
			synchronized (futures) {
				if (!done.get()) {
					futures.add(f);
				}
			}
		}

		@Override
		public void run() {
			try {
				r.run();
			}
			finally {
				synchronized (futures) {
					if (f != null) {
						futures.remove(f);
					}
					done.set(true);
				}

			}
		}
	}
}
