package org.bndtools.rt.executor;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.bndtools.rt.executor.ExecutorImpl.Config;
import org.bndtools.rt.executor.ExecutorImpl.Config.Type;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;

import static java.lang.Math.*;

@Component(designateFactory = Config.class, configurationPolicy = ConfigurationPolicy.require, servicefactory = true)
public class ExecutorImpl implements Executor {

	interface Config {
		enum Type {
			FIXED, CACHED, SINGLE
		}

		int serviceRanking();

		Type type();

		String id();

		int size();
	}
	
	class EsHolder {
		ExecutorService staticEs;
		int counter;
		
		public EsHolder(Map<String, Object> properties) {
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
		
		public ExecutorService getEs() {
			counter ++;
			return staticEs;
		}
		
		public void unget() {
			counter --;
			if(counter==0) {
				staticEs.shutdownNow();
			}
		}
	}
	
	static Map<String, EsHolder> holders = new HashMap<String,ExecutorImpl.EsHolder>();

	List<Future< ? >>		futures			= new ArrayList<Future< ? >>();
	ExecutorService			es;
	
	@Activate
	void activate(Map<String,Object> properties) {
		
		synchronized (holders) {
			String pid = (String) properties.get("service.pid");
			if (!holders.containsKey(pid)) {
				holders.put(pid, new EsHolder(properties));
			}
			es = holders.get(pid).getEs();
		}
		
	}

	@Deactivate
	void deactivate(Map<String,Object> properties) {
		
		synchronized (futures) {
			for (Future< ? > f : futures) {
				f.cancel(true);
			}
		}
		
		synchronized(holders) {
			String pid = (String) properties.get("service.pid");
			holders.get(pid).unget();
			if(holders.get(pid).counter == 0) {
				holders.remove(pid);
			}
		}
		
	}

	@Override
	public void execute(Runnable command) {
		Wrapper w = new Wrapper(command);
		Future<?> f = es.submit(w.r);
		w.setFuture(f);
	}

	class Wrapper {

		Runnable		r;
		Future< ? >		f		= null;
		AtomicBoolean	done	= new AtomicBoolean();

		Wrapper(final Runnable command) {
			r = new Runnable() {

				@Override
				public void run() {
					try {
						command.run();
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
			};
		}

		void setFuture(Future< ? > f) {
			this.f = f;
			synchronized (futures) {
				if (!done.get()) {
					futures.add(f);
				}
			}
		}
	}
}
