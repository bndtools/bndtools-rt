package org.example.tests;

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

public abstract class AbstractDelayedTestCase extends TestCase {

	private static final AtomicBoolean initialised = new AtomicBoolean(false);
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		boolean needToWait = initialised.compareAndSet(false, true);
		if (needToWait) {
			System.out.println("Tests1 waiting 10 seconds for system to settle.");
			Thread.sleep(10000);
			System.out.println("Waiting done, proceeding with tests");
		}
	}
	
}
