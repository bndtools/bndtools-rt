package org.bndtools.rt.executor.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.Executor;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class TestExecutor extends TestCase {

	ServiceReference<?> ref = null;
	Executor e = null;
	Runnable longTask = new Runnable() {
		
		@Override
		public void run() {
			try {
				Thread.sleep(20000);
			} catch (InterruptedException e) {
				System.out.println(" -> Interrupted");
			}
		}
	};
	
	static BundleContext context = FrameworkUtil.getBundle(TestExecutor.class)
			.getBundleContext();

	
	static {
		try {
			ConfigurationAdmin confAdmin = (ConfigurationAdmin) context
					.getService(context.getServiceReference(ConfigurationAdmin.class));
			Configuration conf_fixed_2 = confAdmin.createFactoryConfiguration("org.bndtools.rt.executor.ExecutorImpl", null);
			Configuration conf_single = confAdmin.createFactoryConfiguration("org.bndtools.rt.executor.ExecutorImpl", null);
			
			Dictionary<String, Object> properties = new Hashtable<String, Object>();
			properties.put("type", "FIXED");
			properties.put("size", 2);
			properties.put("service.ranking", 10);
			conf_fixed_2.update(properties);
			
			properties.put("type", "SINGLE");
			properties.remove("size");
			properties.put("service.ranking", 0);
			conf_single.update(properties);
			
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		
		
	}

	public void setUp() throws Exception {		
		ref = context.getServiceReference(Executor.class.getName());
		e = (Executor) context.getService(ref);
	}
	
	public void tearDown() {
		context.ungetService(ref);
	}
	
	public void test_service_found() throws Exception {
		assertNotNull(e);
	}

	public void test_task_executed() throws Exception {
		
		Runnable r = mock(Runnable.class);
		e.execute(r);
		verify(r).run();
		
	}
	

	public void test_single_thread_executor() throws Exception {
		ServiceReference<?> refs[] = context.getServiceReferences(Executor.class.getName(), "(type=SINGLE)");
		
		assertNotSame("No single thread executor registered", null, refs);
		
		e = (Executor) context.getService(refs[0]);

		Runnable r = mock(Runnable.class);
		
		e.execute(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(20000);
				} catch (InterruptedException e) {
					System.out.println(" --> Interrupted");
				}
			}
		});

		e.execute(r);

		verify(r, never()).run();
		
		context.ungetService(refs[0]);
	}

	public void test_two_threads_executor() throws Exception {

		assertNotNull(e);

		Runnable r = mock(Runnable.class);
		
		e.execute(longTask);
		e.execute(longTask);

		e.execute(r);

		verify(r, never()).run();

	}
	
	public void test_clean_up_cancels_tasks() throws Exception {
		Bundle b1 = null;
		Bundle[] bundles = context.getBundles();
		for(Bundle b : bundles) {
			if(b.getSymbolicName().equals("org.bndtools.rt.executor.test.b1")){
				b1 = b;
			}
		}
		assertNotNull(b1);
		b1.start();
		
		BundleContext b1Context = b1.getBundleContext();
		
		Executor eB1 = (Executor)b1Context.getService(ref);
		assertNotNull(eB1);
		
		Runnable r = mock(Runnable.class);
		
		eB1.execute(longTask);
		eB1.execute(longTask);
		
		e.execute(r);
		
		b1.stop();
		
		Thread.sleep(200);
		
		verify(r).run();
	}
	
	public void test_shared_instance() throws Exception {
		Bundle b1 = null;
		Bundle[] bundles = context.getBundles();
		for(Bundle b : bundles) {
			if(b.getSymbolicName().equals("org.bndtools.rt.executor.test.b1")){
				b1 = b;
			}
		}
		assertNotNull(b1);
		b1.start();
		
		BundleContext b1Context = b1.getBundleContext();
		
		Executor eB1 = (Executor)b1Context.getService(ref);
		assertNotNull(eB1);
		
		Runnable r = mock(Runnable.class);
			
		eB1.execute(longTask);
		eB1.execute(longTask);
		
		e.execute(r);
		
		verify(r, never()).run();
		
		b1.stop();
		Thread.sleep(200);
		
		verify(r).run();
	}
}