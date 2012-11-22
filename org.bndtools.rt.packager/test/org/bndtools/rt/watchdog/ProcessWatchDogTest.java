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
package org.bndtools.rt.watchdog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.bndtools.rt.watchdog.manager.WatchDogManager;
import org.bndtools.service.packager.PackageDescriptor;
import org.bndtools.service.packager.ProcessGuard;
import org.bndtools.service.packager.ProcessGuard.State;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import test.MainTest_01;
import test.MainTest_02;
import aQute.lib.io.IO;


public class ProcessWatchDogTest extends TestCase {
	File		bin			= IO.getFile("bin");
	File		test_bin	= IO.getFile("bin_test");
	final File	tmp			= IO.getFile("tmp");

	public void setUp() {
		IO.delete(tmp);

	}

	public void tearDown() throws Exception {
		IO.copy(IO.getFile("tmp/log"), System.out);
		IO.delete(tmp);
	}
	
	// Wait up to <code>secs</code> seconds for the state to change; fail if it hasn't changed by then.
	private void assertStateRefLivenessChange(boolean expected, AtomicReference<State> stateRef, int secs) throws InterruptedException {
		for (int i = 0; i < secs; i++) {
			Thread.sleep(1000);
			if (stateRef.get().isAlive() == expected)
				return;
		}
		throw new AssertionFailedError();
	}

	/**
	 * Simple case of a continuous process that is stopped by killing it. The
	 * process is defined in {@link MainTest_01}.
	 * 
	 */
	public void testSimple() throws Exception {
		PackageDescriptor pd = new PackageDescriptor();
		pd.startScript = "exec java -cp " + bin.getAbsolutePath()
				+ File.pathSeparator + test_bin.getAbsolutePath() + " "
				+ MainTest_01.class.getName();

		final AtomicReference<State> ref = new AtomicReference<State>(
				State.WAITING_FOR_TYPE);

		ProcessGuard guard = new ProcessGuard() {

			@Override
			public Map<String, Object> getProperties() {
				return null;
			}

			@Override
			public void state(State state) throws Exception {
				System.out.println(state);
				ref.set(state);
			}

		};

		WatchDogManager wm = new WatchDogManager(tmp, guard, pd, new File(
				"generated/org.bndtools.rt.packager.test-watchdog.jar"));
		wm.start();

		assertStateRefLivenessChange(true, ref, 300);
		File file = IO.getFile("tmp/work/test");
		assertTrue(file.isFile());
		String s = IO.collect(file);
		assertEquals("A", s);

		wm.close();

		assertFalse(ref.get().isAlive());
	}

	/**
	 * Test what happens when a process continuously quits immediately. The
	 * manager must continue to try to start with a gradual longer period in
	 * between.
	 */
	public void testBrokenProcess() throws Exception {
		PackageDescriptor pd = new PackageDescriptor();
		pd.startScript = "exec java -cp " + bin.getAbsolutePath()
				+ File.pathSeparator + test_bin.getAbsolutePath() + " "
				+ MainTest_02.class.getName();

		final AtomicReference<State> ref = new AtomicReference<State>(
				State.WAITING_FOR_TYPE);
		final AtomicInteger count = new AtomicInteger();

		ProcessGuard guard = new ProcessGuard() {

			@Override
			public Map<String, Object> getProperties() {
				return null;
			}

			@Override
			public void state(State state) throws Exception {
				System.out.println(state);
				ref.set(state);
				count.incrementAndGet();
			}

		};

		WatchDogManager wm = new WatchDogManager(tmp, guard, pd, new File(
				"generated/org.bndtools.rt.packager.test-watchdog.jar"));
		wm.start();

		assertFalse(ref.get().isAlive());

		Thread.sleep(15000);
		assertFalse(ref.get().isAlive());

		assertTrue(count.get() > 3);

		wm.close();

		assertFalse(ref.get().isAlive());
	}

	/**
	 * Test what happens when we kill the manager and try to reattach
	 * with another manager. The child process should continue to live
	 * for the timeout period.
	 */
	public void testReattach() throws Exception {
		PackageDescriptor pd = new PackageDescriptor();
		pd.startScript = "exec java -cp " + bin.getAbsolutePath()
				+ File.pathSeparator + test_bin.getAbsolutePath() + " "
				+ MainTest_01.class.getName();

		final List<State> states = new ArrayList<State>();
		final AtomicReference<State> last = new AtomicReference<State>(
				State.WAITING_FOR_TYPE);

		ProcessGuard guard = new ProcessGuard() {

			@Override
			public Map<String, Object> getProperties() {
				return null;
			}

			@Override
			public void state(State state) throws Exception {
				System.out.println(state);
				last.set(state);
				states.add(state);
			}
		};

		WatchDogManager wm = new WatchDogManager(tmp, guard, pd, new File(
				"generated/org.bndtools.rt.packager.test-watchdog.jar"));
		wm.start();
		assertStateRefLivenessChange(true, last, 10);

		int pid = wm.pid();
		assertTrue( pid > 1);
		
		wm.setNoQuitAtClose();
		wm.close();
		
		// Assert that the process is still alive
		assertTrue(last.get().isAlive());
		
		// Get a new manager to see if it can attach
		WatchDogManager wm2 = new WatchDogManager(tmp, guard, pd, new File(
				"generated/org.bndtools.rt.packager.test-watchdog.jar"));
		
		wm2.start();
		Thread.sleep(2000);

		// should have the same pid
		assertEquals(pid, wm2.pid());
		
		// now we try the same with different configurations
		wm2.setNoQuitAtClose();
		wm2.close();
		
		pd.statusScript = "echo";
		WatchDogManager wm3 = new WatchDogManager(tmp, guard, pd, new File(
				"generated/org.bndtools.rt.packager.test-watchdog.jar"));
		
		wm3.start();
		Thread.sleep(2000);
		// should be running again
		assertTrue(last.get().isAlive());
		
		// but in a different process!
		assertTrue(pid != wm3.pid());
		wm3.close();
		assertFalse(last.get().isAlive());
	}

	/**
	 * 
	 */
}
