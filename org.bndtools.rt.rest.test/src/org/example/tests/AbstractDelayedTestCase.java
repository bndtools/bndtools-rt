package org.example.tests;

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

public abstract class AbstractDelayedTestCase extends TestCase {

	private static final int DELAY_SECS = 3;
	
	private static final AtomicBoolean initialised = new AtomicBoolean(false);
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		boolean needToWait = initialised.compareAndSet(false, true);
		if (needToWait) {
			System.out.printf("Tests1 waiting %d seconds for system to settle.%n", DELAY_SECS);
			Thread.sleep(DELAY_SECS * 1000);
			System.out.println("Waiting done, proceeding with tests");
		}
	}
	
}
