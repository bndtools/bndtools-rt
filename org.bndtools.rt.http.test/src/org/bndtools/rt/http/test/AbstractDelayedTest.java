package org.bndtools.rt.http.test;

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

public abstract class AbstractDelayedTest extends TestCase {

	private static final AtomicBoolean initialised = new AtomicBoolean(false);
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		boolean needToWait = initialised.compareAndSet(false, true);
		if (needToWait) {
			System.out.println("Tests1 waiting 3 seconds for system to settle.");
			Thread.sleep(3000);
			System.out.println("Waiting done, proceeding with tests");
		}
	}
	
}
