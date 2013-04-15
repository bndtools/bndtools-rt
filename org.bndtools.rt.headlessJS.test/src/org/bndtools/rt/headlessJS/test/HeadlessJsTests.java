package org.bndtools.rt.headlessJS.test;

import java.net.URL;
import java.net.URLConnection;

import junit.framework.TestCase;

import org.bndtools.rt.headlessJS.test.testPages.TestServlet;
import org.bndtools.service.headlessJS.HeadlessJS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public class HeadlessJsTests extends TestCase {

	private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
	
	
	public void testBasic() throws Exception {
		ServiceReference<HeadlessJS> ref = context.getServiceReference(HeadlessJS.class);
		HeadlessJS headlessJS = context.getService(ref);
		
		synchronized(this) {
			wait();
		}
		
		URLConnection conn = new URL("http://localhost:8080/test").openConnection(); 
		
		System.out.println(conn.getContentType());
		
		String raw = (String) conn.getContent();
		
		assertEquals(TestServlet.rawPage, raw);
	}
	
}
