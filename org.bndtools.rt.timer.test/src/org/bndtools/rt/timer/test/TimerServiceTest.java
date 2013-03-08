package org.bndtools.rt.timer.test;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * Test case for the timer service
 * 
 */
public class TimerServiceTest extends TestCase {

    private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    /**
     * Verify that bundle {@link org.bndtools.rt.timer.test.b1} (requesting an instance of the timer)
     * is installed in the framework.
     */
	public void test_Bundle1_installed() {
		Bundle b1 = null;
		Bundle[] bundles = context.getBundles();
		for(Bundle b : bundles) {
			if(b.getSymbolicName().equals("org.bndtools.rt.timer.test.b1")){
				b1 = b;
			}
		}
		assertNotNull(b1);
	}
	
	/**
	 * Check that the timer service can be found in the framework.
	 * @throws Exception
	 */
    public void test_timer_service_found() throws Exception {
        ServiceReference serviceReference = context.getServiceReference(Timer.class.getName());
        assertNotNull(serviceReference); 
    }
    
    /**
     * Verify that bundles B1 and main get their own instance of the timer service.
     * @throws Exception
     */
    public void test_different_service_instances() throws Exception {
    	Bundle b1 = null;
		Bundle[] bundles = context.getBundles();
		for(Bundle b : bundles) {
			if(b.getSymbolicName().equals("org.bndtools.rt.timer.test.b1")){
				b1 = b;
				break;
			}
		}
		BundleContext b1Context = b1.getBundleContext();
		ServiceReference refMain = context.getServiceReference(Timer.class.getName());
		ServiceReference refB1 = b1Context.getServiceReference(Timer.class.getName());
		
		assertSame(refMain, refB1);
		assertNotSame(context.getService(refMain), b1Context.getService(refB1));
    }
    
    /**
     * Verification of the timer's normal behavior
     * @throws Exception
     */
    public void test_timertask_fires() throws Exception {
    	Timer timer = null;
    	ServiceReference ref = context.getServiceReference(Timer.class.getName());
    	if(ref != null) {
    		timer = (Timer) context.getService(ref);
    	}
    	if(timer == null) {
    		assert false;
    	}
    	
    	final Semaphore sem = new Semaphore(0);
    	TimerTask task = new TimerTask() {
			
			@Override
			public void run() {
				sem.release();
			}
		};
		
		timer.schedule(task, 1000);
			
		assertTrue(sem.tryAcquire(3000, TimeUnit.MILLISECONDS));	
		
    }
    
    /**
     * Check that a timer service instance is cleaned up when the requesting bundle ungets it.
     * The timer itself is canceled, and the service instance becomes ready for garbage collection.
     * @throws Exception
     */
    public void test_timer_canceled_bundle_uninstall() throws Exception {
    	Bundle b1 = null;
		Bundle[] bundles = context.getBundles();
		for(Bundle b : bundles) {
			if(b.getSymbolicName().equals("org.bndtools.rt.timer.test.b1")){
				b1 = b;
				break;
			}
		}
		BundleContext b1Context = b1.getBundleContext();
		Timer t = (Timer) b1Context.getService(b1Context.getServiceReference(Timer.class.getName()));
		WeakReference<Timer> tt = new WeakReference<Timer>(t);
	
		t = null;
				
		WeakReference<TimerTask> ttask = new WeakReference<TimerTask>(new TimerTask() {

			@Override
			public void run() {
				
			}
		});
		
		tt.get().schedule(ttask.get(), 5000);
		
		b1.uninstall();
		
		Runtime.getRuntime().gc();
		
		assertNull(ttask.get());
		assertNull(tt.get());
    }
}
