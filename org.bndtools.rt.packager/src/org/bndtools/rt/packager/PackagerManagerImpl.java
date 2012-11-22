/*******************************************************************************
 * Copyright (c) 2012 Paremus Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Paremus Ltd - initial API and implementation
 ******************************************************************************/
package org.bndtools.rt.packager;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.bndtools.rt.watchdog.manager.WatchDogManager;
import org.bndtools.rt.watchdog.process.ProcessWatchDog;
import org.bndtools.service.packager.PackageDescriptor;
import org.bndtools.service.packager.PackageType;
import org.bndtools.service.packager.PackagerManager;
import org.bndtools.service.packager.ProcessGuard;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.lib.converter.Converter;
import aQute.lib.io.IO;


/**
 * Package Manager Impl </p> The Packager Manager Impl class implements the
 * Packager Manager service. It tracks {@link PackageType} and
 * {@link ProcessGuard} services. For each found {@link ProcessGuard} it creates
 * a {@link GuardTracker} object, maintained int {@link #watchdogs}. It then
 * matches up the {@link GuardTracker} with a matching {@link PackageType}
 * service through their {@link ProcessGuard#TYPE} and
 * {@link PackageType#PACKAGE_TYPE} service properties.
 * <p/>
 * If a valid type is available, the {@link PackageType} service is asked to
 * provide a {@link PackageDescriptor} object based on the properties from the
 * {@link ProcessGuard#getProperties()} method. Since the the Package Type
 * service needs to store its executable somewhere it provides a data area for
 * the Package Type service to store these executables, scripts, and other
 * external process information. This area is reserved for the Package Type
 * implementer and is cached between framework invocations.
 * <p/>
 * The Packager Manager then creates a directory based on the
 * {@link Constants#SERVICE_PID} configuration property (this matches well to
 * DS's way of handling configuration with {@link ManagedServiceFactory}
 * objects).
 * <p/>
 * Then, a {@link WatchDogManager} instance is created, this object is
 * responsible for managing a <i>Watch Dog</i> process. This is not the real
 * process process but the process that creates and tracks the external process
 * while communicating with the Packager Manager. The reason for this separation
 * is that it allows the Watch Dog process to time out and clean up when it
 * looses contact with its Manager.
 * <p/>
 * The Watch Dog Manager uses the Package Descriptor then to create scripts that
 * start, stop, and ping the external process. These scripts are used by the
 * Watch Dog process to control the actual external process. A proper work
 * directory and log files are also prepared.
 * <p/>
 * A new {@link ProcessWatchDog} process is then called. This implementations
 * ensures that the file system holds the JAR for this process and it creates a
 * LAUNCH script in the process directory that launches the watchdog, giving the
 * process directory as a parameter to the {@link ProcessWatchDog} class.
 * <p/>
 * The impl then creates a {@code lock} file, empty. The {@link ProcessWatchDog}
 * , when its {@link ProcessWatchDog#main(String[])} method is called, will
 * check the lock file, and if exists, will set it to its:
 * <ul>
 * <li>port</li>
 * <li>pid</li>
 * <li>GUID</li>
 * </ul>
 * The Process Watch Dog will then start a back ground thread that communicates
 * back to its manager. The manager picks up the information from the lock file
 * to find the port. The manager then monitors the progress of the watchdog and
 * regularly pings the process, informing the {@link ProcessGuard} about the
 * assumed state of the external process.
 * 
 * <pre>
 *       +---------------+          +--------------+        +-------------+
 *       | Package Type  | n ---> 1 |Package Manag |1 <---n |Process Guard|
 *       +---------------+          +--------------+        +-------------+
 *                                         1                      .
 *                                         |                      .
 *                                         n                      .
 *                                  +--------------+        +-------------+
 *                                  |Guard Tracker |        | ext. prcess |
 *                                  +--------------+        +-------------+
 *                                         1                      1
 *                                         |                     -/-
 *                                        0,1                     1
 *                                  +--------------+        +-------------+
 *                                  |Watchdog Manag| 1 -/- 1|Proc.WatchDog| 
 *                                  +--------------+        +-------------+
 * </pre>
 * 
 * The Watch Dog process is requested to die when either the Package Type or
 * Process Guard is unregistered. The Watch Dog manager can, however, be
 * requested to not kill the Process Watch Dog when
 * {@link WatchDogManager#setNoQuitAtClose()} is set. If a Watch Dog Manager is
 * then subsequently created on the same directory it will actually reattach the
 * existing process if it still ran. If configuration had changed, it would,
 * however, restart the process.
 * <p/>
 * The Process Watch Dog must have a manager. If it is not pinged for a certain
 * amount of time it will automatically die to prevent zombie processes. Zombies
 * are bad because they can block ports and require manual intervention. The
 * Process Watch Dog will go out of its way to kill the external process. The
 * number of seconds a Process Watch Dog may live without a message from its
 * manager can be set on the manager with
 * {@link WatchDogManager#setUnattended(int)}, this is delivered on the next
 * ping message.
 */
@Component(name = "org.bndtools.rt.packager.manager", immediate = true, designate = PackagerManagerImpl.Config.class, configurationPolicy = ConfigurationPolicy.optional)
public class PackagerManagerImpl implements PackagerManager {
	/**
	 * Metatype based configuration data for this implementation.
	 */
	public interface Config {
		@AD(deflt = "pmstorage", description = "Base directory for managing the Package Type's storage of scripts, executables, "
				+ "etc. Should survive framework restarts but acts as a disposable cache. "
				+ "This directory is relative to the framework's working directory.")
		String storage();
	}

	Config								config;

	/**
	 * A {@link GuardTracker} matches each registered {@link ProcessGuard}
	 * service.
	 */
	final List<GuardTracker>			watchdogs	= new ArrayList<GuardTracker>();
	final AtomicReference<LogService>	logRef		= new AtomicReference<LogService>();
	final Map<String, PackageType>		types		= new HashMap<String, PackageType>();

	/**
	 * Storage directory as defined by {@link Config#storage()}
	 */
	File								storage;

	/**
	 * Sub directory of {@link #storage} to hold the process information in
	 * directories named after their PID (in general the calculated PID from a
	 * {@link ManagedServiceFactory}.
	 */
	File								processDir;

	/**
	 * Storage area for types to store their scripts and executables. TODO
	 * currently sub dirs have the name of the types, need thinking about
	 * versions
	 */
	File								typeDir;

	/**
	 * A file that points to the JAR used for the Watch Dog process. This JAR is
	 * created by the {@code ./bnd/watchdog.bnd} file during build as instructed
	 * by the bnd file's {@code Include-Resource} instruction. Notice that we
	 * (confusingly) also have an identical jar being built in generated for
	 * testing.
	 */
	File								watchdog;

	/**
	 * The GuardTracker is a tuple that collects the guard, type and related
	 * info. It can be opened and closed depending on the availability of the
	 * type handler. If it is open, it will create a {@link WatchDogManager}.
	 * The atomicity is guaranteed (hopefully) by the synchronized methods that
	 * manage the types.
	 * <p/>
	 * The key problem is that the presence of the process depends on the pair
	 * of guard x type. This class tries to simplify managing the dynamic
	 * movements of either. It is created when a guard is found but it is
	 * opened/closed based on the associated type.
	 */
	class GuardTracker {
		String			type;
		ProcessGuard	guard;
		PackageType		handler;
		WatchDogManager	wm;
		String			pid;
		File			dir;
		File			data;

		/**
		 * Atomically open the process.
		 * 
		 * @throws Exception
		 */
		public synchronized void open() throws Exception {
			if (wm == null && handler != null) {
				PackageDescriptor pd = handler.create(guard.getProperties(),
						data);
				wm = new WatchDogManager(dir, guard, pd, watchdog);
				wm.setDaemon(true);
				wm.start();
			}
		}

		/**
		 * Atomically close the process.
		 * 
		 * @throws Exception
		 */
		public synchronized void close() throws IOException {
			if (wm != null) {
				wm.close();
				wm = null;
			}
		}

		/**
		 * Check if this object needs the type or is already satisfied. I.e.
		 * does this wants to be opened? This is done so a synchronized block
		 * can find out who needs to be opened and then open it outside the sync
		 * block. A bit like lock crabbing.
		 * 
		 * @param type
		 *            The type that has arrived
		 * @param pt
		 *            The object that could handle it
		 * @return true if this object wants to be opened.
		 */
		synchronized boolean needs(String type, PackageType pt) {
			if (this.type.equals(type) && handler == null) {
				this.handler = pt;
				return wm == null;
			}
			return false;
		}

		/**
		 * Indicate if this object needs to be closed when the package type is
		 * gone. If returning true, it will soon be closed.
		 * 
		 * @param pt
		 *            The Package Type that is unregistering
		 * @return true if this object currently depends on the package type.
		 */
		synchronized boolean gone(PackageType pt) {
			if (handler == pt) {
				handler = null;
				return wm != null;
			}
			return false;
		}
	}

	/**
	 * Tracks the guards. This is not done with a @Reference for the simple
	 * reason that we're not initalized yet when they come in. The service
	 * tracker allows us to do this after initialization.
	 */
	ServiceTracker<ProcessGuard, GuardTracker>	tracker;

	/**
	 * Main function to activate this component. 
	 * <p/>
	 * This will start a tracker to
	 * track the {@link ProcessGuard} services. Don't even think of replacing
	 * this tracker with an @Reference annotation method since this will fail.
	 * These methods are called before the {@link #activate(BundleContext, Map)}
	 * method is called and then use uninitialized things.
	 */
	@Activate
	void activate(BundleContext context, Map<String, Object> properties)
			throws Exception {
		config = Converter.cnv(Config.class, properties);
		if (config.storage() != null)
			storage = IO.getFile(config.storage());
		else
			storage = context.getDataFile("storage");

		storage.mkdirs();
		if (!storage.isDirectory())
			throw new IllegalStateException("Cannot create my storage dir");
		log(LogService.LOG_INFO, String.format("PackageManager using storage directory %s.", storage.getAbsolutePath()));

		typeDir = new File(storage, "types");
		typeDir.mkdir();
		processDir = new File(storage, "processes");
		processDir.mkdir();

		watchdog = IO.getFile(storage, "watchdog.jar");
		IO.copy(context.getBundle().getResource("watchdog.jar"), watchdog);

		tracker = getTracker(context);
		tracker.open();
	}
	
	@Deactivate
	void deactivate() {
		tracker.close();
	}

	/**
	 * Creates a tracker on the {@link ProcessGuard} services. This will
	 * create a {@link GuardTracker} for each found service.
	 */
	private ServiceTracker<ProcessGuard, GuardTracker> getTracker(
			BundleContext context) {
		return new ServiceTracker<ProcessGuard, GuardTracker>(context,
				ProcessGuard.class, null) {
			@Override
			public GuardTracker addingService(ServiceReference<ProcessGuard> ref) {

				String type = null, pid = null;
				try {
					// TODO handle String+
					type = (String) ref.getProperty("type");
				} catch (Exception e) {
					log(LogService.LOG_WARNING,
							"Found type manager with a type that cannot convert to String");
				}
				try {
					// TODO handle String+
					pid = (String) ref.getProperty(Constants.SERVICE_PID);
				} catch (Exception e) {
					log(LogService.LOG_WARNING,
							"Found type manager with a pid that cannot convert to String");
				}

				if (type == null) {
					log(LogService.LOG_WARNING,
							"Found Process Guard without 'type' service property set");
					return null; // don't track
				}

				if (pid == null) {
					log(LogService.LOG_WARNING,
							"Found Process Guard without 'service.pid' set");
					return null; // don't track
				}

				// TODO handle multiple versions, or assume singletons??
				File data = new File(typeDir, type);
				File dir = new File(processDir, pid);
				dir.mkdir();
				data.mkdir();
				
				if (!dir.isDirectory()) {
					log(LogService.LOG_ERROR,
							"Cannot create directory to store process info for "
									+ pid + " in " + dir);
					return null; // don't track
				}
				if (!data.isDirectory()) {
					log(LogService.LOG_ERROR,
							"Cannot create directory to store package type info for "
									+ data);
					return null; // don't track
				}

				ProcessGuard s = context.getService(ref);
				if (s == null) {
					// oops, gone or failed a Service Factory ...
					// usually well reported by DS
					return null;
				}

				GuardTracker gt = new GuardTracker();
				gt.guard = s;
				gt.type = type;
				gt.pid = pid;
				gt.data = data;
				gt.dir = dir;

				synchronized (watchdogs) {
					watchdogs.add(gt);
					gt.handler = types.get(type);
				}
				try {
					// Is dummy if no handler is set
					gt.open();
				} catch (Exception e) {
					e.printStackTrace();
					log(LogService.LOG_ERROR, "Cannot open guard/type  " + gt);
				}
				
				// still tracked since we might be able to open later when a new 
				// type handler gets in
				return gt;
			}

			// TODO modified??

			@Override
			public void removedService(ServiceReference<ProcessGuard> ref,
					GuardTracker gt) {
				
				synchronized (watchdogs) {
					watchdogs.remove(gt);
				}
				try {
					gt.close();
				} catch (IOException e) {
					e.printStackTrace();
					log(LogService.LOG_ERROR, "Cannot close guard/type  " + gt);
				}
			}

		};
	}

	@Reference(type = '*')
	void addPackagerType(PackageType pt, Map<String, Object> props) {
		// TODO handle String+
		String type = (String) props.get(PackageType.PACKAGE_TYPE);
		if (type == null)
			throw new IllegalArgumentException("Must have a property "
					+ PackageType.PACKAGE_TYPE);

		List<GuardTracker> tobeopened = new ArrayList<PackagerManagerImpl.GuardTracker>();
		synchronized (watchdogs) {
			types.put(type, pt);
			for (GuardTracker gt : watchdogs) {
				if (gt.needs(type, pt))
					tobeopened.add(gt);
			}
		}
		for (GuardTracker gt : tobeopened) {
			try {
				gt.open();
			} catch (Exception e) {
				e.printStackTrace();
				log(LogService.LOG_ERROR, "Cannot open guard/type  " + gt);
			}
		}
	}

	void removePackagerType(PackageType pt, Map<String, Object> props) {
		String type = (String) props.get(PackageType.PACKAGE_TYPE);
		if (type == null)
			throw new IllegalArgumentException("Must have a property "
					+ PackageType.PACKAGE_TYPE);

		List<GuardTracker> tobeclosed = new ArrayList<PackagerManagerImpl.GuardTracker>();
		synchronized (watchdogs) {
			for (GuardTracker gt : watchdogs) {
				types.remove(type);
				if (gt.gone(pt)) {
					tobeclosed.add(gt);
				}
			}
		}

		// TODO maybe we should not close them ...
		// since they could live on without their
		// type manager

		for (GuardTracker gt : tobeclosed) {
			try {
				gt.close();
			} catch (IOException e) {
				e.printStackTrace();
				log(LogService.LOG_ERROR, "Cannot close guard/type  " + gt);
			}
		}
	}

	@Reference(type = '?')
	void setLog(LogService log) {
		logRef.set(log);
	}

	void unsetLog(LogService log) {
		logRef.compareAndSet(log, null);
	}

	public synchronized PackageType getType(String type) {
		return types.get(type);
	}

	private void log(int level, String message) {
		LogService log = logRef.get();
		if ( log == null ) {
			PrintStream stream = level <= LogService.LOG_WARNING ? System.err : System.out;
			stream.println(message);
		} else {
			log.log(level, message);
		}
	}
}
