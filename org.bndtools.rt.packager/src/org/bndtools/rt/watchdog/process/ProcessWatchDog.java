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
package org.bndtools.rt.watchdog.process;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bndtools.service.watchdog.Reply;
import org.bndtools.service.watchdog.Request;
import org.bndtools.service.watchdog.Status;

import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.libg.command.Command;


public class ProcessWatchDog extends Thread {
	static long			TIMER_UNATTENDED	= 30000;

	static JSONCodec	codec				= new JSONCodec();
	File				home;
	File				work;
	File				lock;
	File				descriptor;
	Date				last				= new Date();
	DatagramSocket		socket;
	Command				command;
	final UUID			uuid				= UUID.randomUUID();
	boolean				trace				= true;

	volatile int		result				= Integer.MIN_VALUE;
	volatile Status		status				= Status.STARTING;
	volatile String		alert;
	volatile String		ping;

	public static void main(String args[]) throws Exception, SecurityException,
			NoSuchMethodException {
		if (args.length == 0)
			throw new IllegalArgumentException(
					"Must be invoked with a process directory parameter");

		ProcessWatchDog main = new ProcessWatchDog();
		main.dir(args[0]);
	}

	// The main code

	public void dir(String dPath) throws Exception {
		home = new File(dPath).getAbsoluteFile();
		if (!home.isDirectory())
			throw new IllegalArgumentException("No such directory: "
					+ descriptor);

		lock = new File(home, "lock");
		if (!lock.isFile())
			throw new IllegalArgumentException("Cannot find lock file! " + lock);

		lock.deleteOnExit();

		work = new File(home, "work");
		if (!work.mkdir() && !work.isDirectory())
			throw new IllegalArgumentException("Cannot create work directory");

		socket = new DatagramSocket();
		socket.setSoTimeout(5000);

		trace("Port " + socket.getLocalPort());

		File tmplock = new File(lock.getAbsolutePath() + ".tmp");
		tmplock.delete();
		String content = socket.getLocalPort() + ":" + System.getProperty("pid")
				+ ":" + uuid.toString();
		trace("lock content : " + content);
		
		IO.store(content, tmplock);

		if (!tmplock.renameTo(lock)) {
			throw new IllegalArgumentException("Cannot rename : " + tmplock
					+ " to " + lock);
		}

		trace("Written lock file " + lock + " ");


		Runtime.getRuntime().addShutdownHook( new Thread() {
			public void run() {
				System.out.println("Canceling from shutdown hook");
				command.cancel();
			}
		});
		
		
		
		setDaemon(true);
		start(); // starts communicator

		try {

			command = new Command(new File(home, "START").getAbsolutePath());
			command.setCwd(work);
			command.setTimeout(0, TimeUnit.MILLISECONDS);
			result = command.execute(System.out, System.err);
			trace("Command result: " + result);
		} catch (Throwable e) {
			e.printStackTrace();
			alert = stackTrace(e);
		}
		lock.delete();
		trace("Goodbye");

		try {
			interrupt();
			join(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(result);
	}

	@Override
	public void run() {
		try {
			trace("Enter run");
			boolean stopping = false;
			byte[] buffer = new byte[1400];
			DatagramPacket dp = new DatagramPacket(buffer, 1400);

			long deadline = System.currentTimeMillis() + TIMER_UNATTENDED;
			while (true) {
				if ( isInterrupted() ) {
					trace("interrupted");
					break;
				}
					
				
				if ( stopping ) {
					trace("stopping");
					break;
				}
				
				if  ( deadline < System.currentTimeMillis() ) {
					trace("timed out");
					break;
				}

				if ( ! lock.isFile()) {
					trace("no lock file");					
					break;
				}
				
				trace("pinging");					
				doPing();
				
				try {
					trace("Listening for messages on port " + socket.getLocalPort() );
					socket.receive(dp);
					trace("Received message " + dp.getAddress());
					if (dp.getAddress().isLoopbackAddress()) {


						String s = new String(dp.getData(), dp.getOffset(),
								dp.getLength(), "UTF-8");

						trace("Received message " + s);
						Reply reply = new Reply();
						reply.alert = alert;
						reply.message = "";

						try {
							Request rq = codec.dec().from(s).get(Request.class);

							// Every request an carry  the next deadline information.
							if ( rq.next != 0 )
								deadline = System.currentTimeMillis() + rq.next * 1000;
							else
								deadline = System.currentTimeMillis()
									+ TIMER_UNATTENDED;
							
							reply.id = rq.id;
							
							switch (rq.request) {
							case PING:
								break;

							case QUIT:
								stopping = true;
								status = Status.QUITING;
								break;

							case TRACE_ON:
								trace = true;
								break;

							case TRACE_OFF:
								trace = false;
								break;

							default:
								reply.alert = "Unknonwn message type"
										+ rq.request;
							}
						} catch (Throwable t) {
							reply.alert = "Unknown failure in watchdog code: while receiving : "
									+ s + "\n" + stackTrace(t);
						}

						reply.status = status;
						s = codec.enc().put(reply).toString();

						byte data[] = s.getBytes("UTF-8");
						DatagramPacket p = new DatagramPacket(data, 0,
								data.length, dp.getAddress(), dp.getPort());

						trace("Sending reply message " + s);
						socket.send(p);

					} else {
						System.err.println("Received UDP from external source");
						alert = "Received UDP from a non-local host: "
								+ dp.getAddress().getHostAddress();
					}
				} catch (SocketTimeoutException stoe) {
					// Just restarts the loop ...
				}
			}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		} finally {
			trace("timed out");
			status = Status.QUITING;
			try {
				socket.close();
			} finally {
				try {
					trace("quit");
					quit();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Use the ping script to see if we can stop it.
	 */
	private void doPing() {
		File ping = new File(home, "PING");
		if (!ping.isFile()) {
			trace("No ping script ... assume all is ok");
			status = Status.ALIVE;
			this.ping = "ok (though no script)";
			return;
		}

		Command command = new Command(ping.getAbsolutePath());
		try {
			command.setCwd(work);
			command.setTimeout(30, TimeUnit.SECONDS);
			StringBuffer out = new StringBuffer();
			int result = command.execute(out, out);
			if (result == 0) {
				status = Status.ALIVE;
				this.ping = "ok";
				trace("ok ping");
				return;
			}

			if (out.length() > 600) {
				out.delete(600, out.length());
				out.append("...");
			}
			trace("error " + result + " " + out);
			this.ping = out.toString();
			this.status = Status.UNCERTAIN;
		} catch (Exception e) {
			trace("exception in ping " + e.getMessage());
			status = Status.UNCERTAIN;
			alert = stackTrace(e);
		} finally {
			command.cancel();
		}
	}

	private void quit() throws Exception {
		try {
			trace("quiting");
			File stop = new File(home, "STOP");
			if (!stop.isFile()) {
				trace("No stop script ... will kill process");
				status = Status.QUITING;
				return;
			} else {

				Command command = new Command(stop.getAbsolutePath());
				StringBuffer std = new StringBuffer();
				command.setTimeout(10, TimeUnit.SECONDS);
				int result = command.execute(std, std);
				if ( result == 0)
					return;

				trace("error in quit command " + std);
				alert = std.toString();
			}
		} finally {
			trace("canceling command");
			command.cancel();
			System.exit(1);
		}
	}

	private static String stackTrace(Throwable e) {
		StringWriter w = new StringWriter();
		PrintWriter pw = new PrintWriter(w);
		e.printStackTrace(pw);
		return w.toString();
	}

	private void trace(String string) {
		if (trace)
			System.err.println("JPM: " + string);
	}

}
