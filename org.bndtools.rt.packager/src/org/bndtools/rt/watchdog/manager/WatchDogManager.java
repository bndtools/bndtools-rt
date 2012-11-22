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
package org.bndtools.rt.watchdog.manager;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bndtools.rt.watchdog.manager.WatchDogManager.History.Event;
import org.bndtools.rt.watchdog.process.ProcessWatchDog;
import org.bndtools.service.packager.PackageDescriptor;
import org.bndtools.service.packager.ProcessGuard;
import org.bndtools.service.watchdog.Reply;
import org.bndtools.service.watchdog.Request;
import org.bndtools.service.watchdog.Status;

import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.libg.command.Command;


/**
 * A Watch Dog Manager is responsible for managing a {@link ProcessWatchDog}
 * that runs in a different process. The Process Watch Dog will die when it is
 * not regularly communicated with (pinged mainly), which is done by this class.
 * This class also prepares the scripts and file system for the Process Watch
 * Dog to run smoothly.
 * 
 * <p/>
 * This class creates a thread that implements the (complex) state machine to
 * keep the Process Watch Dog (and by implication its child process) alive as
 * long as this instance is not closed. The thread should be started.
 */
public class WatchDogManager extends Thread implements Closeable {
	static final String			LOCK									= "lock";
	/**
	 * TODO tune the timers ...
	 */
	static final long			S										= 1000;
	static final long			TIMER_WAIT_FOR_THREAD_TO_FINISH			= 15 * S;
	private static final long	TIMER_NOT_ALIVE							= 30 * S;
	private static final long	TIMER_WAIT_FOR_QUIT						= 31 * S;
	private static final long	TIMER_LAUNCHING_FAILED					= 5 * S;
	private static final long	TIMER_UNRECOGNIZED_FAILURE				= 30 * S;
	private static final long	TIMER_WAIT_FOR_WATCHDOG_TO_GET_STARTED	= 30 * S;
	private static final long	TIMER_PING								= 10 * S;
	private static final long	TIMER_WAIT_FOR_LOCK_CONTENT				= 30 * S;

	/*
	 * Format of lock file: port:pid:GUID ...
	 */
	static Pattern				INFO_LINE								= Pattern
																				.compile("(\\d+):(\\d+):(.*)");

	/*
	 * Counter for message ids.
	 */
	static AtomicInteger		unique									= new AtomicInteger(
																				1);
	static JSONCodec			codec									= new JSONCodec();

	final PackageDescriptor		descriptor;
	final ProcessGuard			guard;
	final File					dir;
	final File					watchdog;

	Command						command;
	int							port;
	int							pid;
	ProcessGuard.State			state									= ProcessGuard.State.WAITING_FOR_TYPE;
	File						lock;

	/**
	 * History maintains the last 200 events for debugging purposes
	 */
	public static class History {
		public enum Event {
			INIT, DETACHED, INTERRUPTED, SLEEP, CHECKFAILED, LAUNCH, SETUP, ATTACH, REATTCH, PING, QUIT, LOST, EXIT, ALERT, ERROR, FAILED, CLOSE, GUARD, NOLOCK, TIMEOUT, CHECKOK, FINISHED, PINGOK, PINGFAIL, SEND, RECEIVED;
		}

		public long					time;
		public String				what;
		public ProcessGuard.State	status;
		public Event				event;
		public int					pid;
		public int					port;

		public String toString() {
			Formatter f = new Formatter();
			f.format("%-10s %-10s %s", event, status, what);
			return f.toString();
		}
	}

	// Used reversed to show last element first
	final LinkedList<History>	history	= new LinkedList<History>();

	/*
	 * If set, leave the process running and do not quit it.
	 */
	boolean						noQuitAtClose;

	/*
	 * Number of seconds before the process dies because it feels orphaned. If
	 * 0, the process picks its own default. Used in testing and special cases.
	 * 
	 * TODO needs to make this configurable?
	 */
	private int					unattendedSeconds;
	private boolean				trace	= false;

	/**
	 * Create a (not running) watch dog manager. Don't forget to start it.
	 */
	public WatchDogManager(File dir, ProcessGuard guard,
			PackageDescriptor descriptor, File watchdog) throws Exception {
		super("WatchdogManager-" + dir);
		this.guard = guard;
		this.dir = dir;
		dir.mkdirs();
		if (!dir.isDirectory())
			throw new IllegalArgumentException(
					"Cannot not create setup directory " + dir);
		this.watchdog = watchdog;
		this.lock = new File(dir, LOCK);
		this.descriptor = descriptor;
	}

	/**
	 * Main run. this method will attach an existing process or launch a new
	 * process depending on the guard, type and running state. This method has
	 * one purpose: to keep the process running by tracking it, restarting it or
	 * whatever until this object is closed, after which we ask it to quit.
	 */
	public void run() {
		try {
			int tries = 1;

			while (!isInterrupted())
				try {
					state(ProcessGuard.State.WAITING_FOR_TYPE);

					boolean running = attach("reattach existing process");
					boolean valid = check();

					if (running && !valid) {
						quit("reattch due to invalid setup");
						running = false;
					}

					if (!valid) {
						setup();
						valid = true;
					}

					while (!running) {

						//
						// We are now attempting to start the process
						// and then attach it.
						//

						state(ProcessGuard.State.STARTING);
						if (launch())
							running = attach("launched process");

						//
						// the attach either succeeded or failed
						//

						if (!running) {
							state(ProcessGuard.State.FAILED);

							//
							// Do a longer and longer backoff to minimize
							// the load on the system
							//

							sleep("Grace period after launch/attach failure",
									TIMER_LAUNCHING_FAILED * tries++);
						}
					}

					//
					// We can now conclude we're started. Inform the
					// user and reset our backoff counter
					//

					state(ProcessGuard.State.STARTED);
					tries = 1;
					long deadline = deadline(TIMER_NOT_ALIVE);

					//
					// Now keep on assuming the process lives
					// which is when the lock file exists (the
					// watchdog is set to kill it) and we
					// can ping before the timeout
					//

					while (running && lock.isFile()) {

						if (ping()) {

							deadline = deadline(TIMER_NOT_ALIVE); // reset timer
							state(ProcessGuard.State.PINGED);

						} else {

							if (deadline < now()) {

								quit("no valid alive received before deadline");
								running = false;
								break;
							}
						}
						sleep("Wait for the next ping time", TIMER_PING);
					}

					//
					// We're no longer running as we should be since
					// we get an interrupt when we're closed. The
					// loop will start again which should restart the
					// process
					//
					
					state(ProcessGuard.State.FAILED);
					
				} catch (InterruptedException e) {
					history(Event.INTERRUPTED, "will leave inner run");
					return;
				} catch (Exception e) {
					e.printStackTrace(); // TODO
					try {
						//
						// We're now in unknown territory since our own
						// code is throwing exceptions. We keep on trying
						// but should do a gradual backoff to minimize system
						// load.
						//
						sleep("internal error " + e, TIMER_UNRECOGNIZED_FAILURE
								* tries++);

					} catch (InterruptedException e1) {
						history(Event.INTERRUPTED,
								"waiting for error grace period");
						return;
					}
				}
		} finally {
			//
			// We can quit by leaving the process running ... we will
			// then reattach later (or the process dies due to lack of
			// attention
			//
			if (noQuitAtClose) {
				history(Event.DETACHED, "leave process detached");
				return;
			}

			quit("leaving manager");
		}
	}

	/*
	 * Calculate deadline
	 */
	private long deadline(long delta) {
		return now() + delta;
	}

	/*
	 * System.currentTimeMillis is soooooo long
	 */
	private long now() {
		return System.currentTimeMillis();
	}

	/*
	 * Sleep method with event reporting
	 */
	private void sleep(String why, long l) throws InterruptedException {
		history(Event.SLEEP, why + " : " + l);
		try {
			sleep(l);
		} finally {
			history(Event.SLEEP, why + " : done");
		}
	}

	/*
	 * Report to the guard but make sure that any exceptions thrown do not kill
	 * our state machine. And create an event.
	 * 
	 * @param state the state to report.
	 */
	private void state(ProcessGuard.State state) {
		try {
			this.state = state;
			history(Event.GUARD, "");
			guard.state(state);
		} catch (Exception e) {
			history(History.Event.ERROR, "Guard failed " + e.getMessage());
		}
	}

	/**
	 * Stop the thread, well interrupt its run method.
	 */

	public void close() throws IOException {
		history(Event.CLOSE, "");
		this.interrupt();
		try {
			this.join(TIMER_WAIT_FOR_THREAD_TO_FINISH);
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Try to attach to a process. A process exists if:
	 * <ul>
	 * <li>There is a lock file</li>
	 * <li>The lock file has a valid port:pid:guid</li>
	 * <li>The process responds to a ping</li>
	 * </ul>
	 * 
	 * @return
	 */
	boolean attach(String why) {
		history(Event.ATTACH, why);
		File lock = new File(dir, LOCK);
		try {
			if (lock.isFile()) {
				String content = "";
				try {
					content = IO.collect(lock);

					long deadline = deadline(TIMER_WAIT_FOR_LOCK_CONTENT);
					while (content.isEmpty()) {
						
						//
						// It is possible that  our watch dog has not started
						// yet. So we continue to scan the lock file until
						// we see the content. Since this is the first thing
						// the watch dog does, we should not have to wait long
						//
						
						if (deadline < now()) {
							history(Event.NOLOCK,
									"could not find proper lock file content before deadline");
							lock.delete();
							return false;
						}

						sleep(500); 
						content = IO.collect(lock);
					}
				} catch (FileNotFoundException fnfe) {
					//
					// The watch dog kills the lock when it dies,
					// so this can happen any time
					//
					history(Event.NOLOCK, "lock file gone");
					return false;
				}

				//
				// Check the contents of the lock
				// file to get the port and pid
				//
				
				Matcher m = INFO_LINE.matcher(content);
				if (m.matches()) {
					
					port = Integer.parseInt(m.group(1));
					pid = Integer.parseInt(m.group(2));

					long deadline = deadline(TIMER_NOT_ALIVE);
					while (deadline > now()) {

						//
						// Try to ping the watch dog
						//
						
						Reply reply = send(Request.Command.PING);
						if (reply != null) {
							if (reply.status == Status.ALIVE) {
								history(Event.ATTACH, content);
								return true;
							}
						}
					}
					history(Event.TIMEOUT,
							"timeout waiting for attach ping ack");
				}
			} else
				history(Event.NOLOCK, "No lock file");

		} catch (Throwable e) {
			e.printStackTrace(); // TODO
			history(Event.ERROR, "confused: " + e);
		}
		return false;
	}

	/**
	 * Quit the current process. We assume we're attached.
	 * 
	 * @param string
	 * @throws Exception
	 */

	void quit(String message) {
		history(Event.QUIT, message);

		assert port != 0;
		assert pid != 0;
		
		try {
			
			long deadline = deadline(TIMER_WAIT_FOR_QUIT);
			
			while (deadline > now()) {
				if (!lock.isFile()) {
					history(Event.NOLOCK, "No lock file");
					return;
				}

				Reply reply = send(Request.Command.QUIT);
				if (reply != null && reply.status == Status.QUITING) {
					history(Event.QUIT, "Acked");
					return;
				}
			}
			history(Event.QUIT, "Quit; " + message);
		} finally {
			if (command != null)
				command.cancel();

			port = 0;
			pid = 0;
			lock.delete(); // should also kill the watch dog
			state(ProcessGuard.State.STOPPED);
		}
	}

	/**
	 * Check if the configuration has changed from out persistent configuration.
	 * 
	 */

	boolean check() {
		File dfile = new File(dir, "descriptor.json");
		if (dfile.isFile())
			try {
				PackageDescriptor stale = codec.dec().from(dfile)
						.get(PackageDescriptor.class);
				if (equals(stale, descriptor)) {
					history(Event.CHECKOK, "ok");
					return true;
				}
			} catch (Throwable t) {
				dfile.delete();
				history(Event.ERROR, "probaly syntax error in descriptor? " + t );
			}
		history(Event.CHECKFAILED, "failed");
		return false;
	}

	/**
	 * Setup the environment. We create the following scripts for the watchdog:
	 * 
	 * <ul>
	 * <li>launch - launches the watchdog</li>
	 * <li>start</li>
	 * <li>stop</li>
	 * <li>ping</li>
	 * </ul>
	 * 
	 */

	boolean setup() throws IOException {

		File start = new File(dir, "START");
		File stop = new File(dir, "STOP");
		File ping = new File(dir, "PING");
		File launch = new File(dir, "LAUNCH");

		File log = new File(dir, "log");

		try {
			Formatter f = new Formatter(new StringBuilder());
			f.format("exec nohup java -Dpid=$$ -jar %s %s 2>>%s >>%s &",
					watchdog.getAbsolutePath(), dir.getAbsolutePath(),
					log.getAbsolutePath(), log.getAbsolutePath());
			script(launch, f.toString());
			script(start, descriptor.startScript);
			script(stop, descriptor.stopScript);
			script(ping, descriptor.statusScript);

			File desc = new File(dir, "descriptor.json");
			codec.enc().to(desc).put(descriptor);
			history(Event.SETUP, "");
			return true;
		} catch (Throwable e) {
			history(Event.ERROR, "Confused " + e);
			start.delete();
			stop.delete();
			ping.delete();
			launch.delete();
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Launch the watch dog.
	 */
	boolean launch() {
		try {
			File work = new File(dir, "work");
			work.mkdir();
			if (!work.isDirectory())
				throw new IllegalArgumentException("Cannot make work directory");

			// TODO set user rights

			if (!lock.createNewFile()) {
				history(Event.NOLOCK, "Could not create lock file as new");
				throw new Exception("Cannot create lock file " + lock);
			}

			command = new Command(new File(dir, "LAUNCH").getAbsolutePath());
			command.setCwd(dir);
			command.setTimeout(TIMER_WAIT_FOR_WATCHDOG_TO_GET_STARTED,
					TimeUnit.MILLISECONDS);

			// redirect to our IO ... there should be no output except some
			// debug traces
			// which we like to see
			history(Event.LAUNCH, "");
			int result = command.execute(System.out, System.err);
			history(Event.FINISHED, "Result " + result);

			return result == 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * ping the remote process and tell if this worked.
	 */

	boolean ping() {
		try {
			history(Event.PING, "");
			Reply reply = send(Request.Command.PING);
			boolean answer = reply != null && reply.status == Status.ALIVE;
			if (answer)
				history(Event.PINGOK, "");
			else
				history(Event.PINGFAIL, "");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			history(Event.ERROR, "Confused " + e.getMessage());
		}
		return false;
	}

	private void script(File file, String script) throws Exception {
		if (script != null && script.trim().length() > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append("#!/bin/sh\n");
			sb.append(script).append("\n");
			IO.store(sb.toString(), file);
			chmod("a+x", file);
		} else
			file.delete();
	}

	boolean equals(PackageDescriptor a, PackageDescriptor b) {
		try {
			if (a == null && b != null)
				return false;

			if (a == null && b == null)
				return true;

			String sa = codec.enc().put(a).toString();
			String sb = codec.enc().put(b).toString();
			// System.out.println("a&b \n  " + sa + "\n  " + sb);
			return sa.equals(sb);
		} catch (Exception e) {
			return false;
		}
	}

	void chmod(String args, File file) throws Exception {
		Command cmd = new Command("chmod " + args + " "
				+ file.getAbsolutePath());
		StringBuffer out = new StringBuffer();
		int result = cmd.execute(out, out);
		if (result != 0)
			throw new Exception("could not chmod " + file + " result: "
					+ result + " : " + out);
	}

	Reply send(Request.Command cmd) {
		history(Event.SEND, cmd.toString());
		Request request = new Request();
		request.request = cmd;
		request.next = unattendedSeconds;
		request.id = unique.incrementAndGet() + "";
		DatagramSocket socket = null;

		try {
			socket = new DatagramSocket();
			socket.setReceiveBufferSize(5000);
			socket.setSoTimeout(5000);

			String m = codec.enc().put(request).toString();
			history(Event.SEND, m);
			byte data[] = m.getBytes("UTF-8");

			DatagramPacket p = new DatagramPacket(data, 0, data.length,
					InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }), port);

			socket.send(p);

			byte[] buffer = new byte[5000];
			DatagramPacket dp = new DatagramPacket(buffer, 5000);
			socket.receive(dp);

			String r = new String(dp.getData(), dp.getOffset(), dp.getLength(),
					"UTF-8");
			history(Event.RECEIVED, r);
			return codec.dec().from(r).get(Reply.class);
		} catch (SocketTimeoutException stoe) {
			history(Event.TIMEOUT, "waiting for reply");
		} catch (InterruptedException ie) {
			history(Event.INTERRUPTED, "waiting for reply");
		} catch (Exception e) {
			e.printStackTrace();
			history(Event.ERROR, "waiting for reply " + e);
		} finally {
			if (socket != null)
				socket.close();
		}
		return null;
	}

	/**
	 * Inidicate that the process should not be quited when this manager is
	 * closed. This allows another manager to reattach.
	 */
	public void setNoQuitAtClose() {
		this.noQuitAtClose = true;
	}

	/**
	 * Return the PID of the managed watchdog process (not its child process!)
	 * 
	 * @return a pid
	 */
	public int pid() {
		return pid;
	}

	/**
	 * Set history events
	 * 
	 * @param event
	 * @param what
	 * @return
	 */
	public History history(History.Event event, String what) {
		History h = new History();
		h.event = event;
		h.what = what;
		h.pid = pid;
		h.port = port;
		h.time = System.currentTimeMillis();
		h.status = state;
		synchronized (history) {
			history.addFirst(h);
			if (history.size() > 200)
				history.removeLast();
		}
		if (trace)
			System.out.println(h);
		return h;
	}

	/**
	 * Set the time the watchdog process can live unattended in seconds.
	 * 
	 * @param seconds
	 */
	public void setUnattended(int seconds) {
		this.unattendedSeconds = seconds;
	}

	/**
	 * Set trace on.
	 * 
	 * @param on
	 */
	public void setTrace(boolean on) {
		this.trace = on;
	}

	/**
	 * Get the recent history
	 * 
	 */
	public List<History> getHistory() {
		return Collections.unmodifiableList(history);
	}

	public ProcessGuard getGuard() {
		return guard;
	}
}
