package org.bndtools.rt.headlessJS.test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.bndtools.rt.headlessJS.test.testPages.TestServlet;
import org.bndtools.service.headlessJS.HeadlessJS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import aQute.lib.io.IO;


public class HeadlessJsTests extends TestCase {

	private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
	private static final String PAGE = "http://localhost:8080/test";
	
	public void testBasic() throws Exception {
		ServiceReference<HeadlessJS> ref = context.getServiceReference(HeadlessJS.class);
		HeadlessJS headlessJS = context.getService(ref);
		URL url = new URL(PAGE);
		StringWriter w = new StringWriter();
		IO.copy(url.openStream(), w);
		String raw = w.toString();
	
		assertEquals(TestServlet.rawPage, raw);
		
		Thread.sleep(1000); // Giving time for phantomJS to start
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		headlessJS.getInterpretedPage(new URI(PAGE), out);
		String interpreted = out.toString();
		
		System.out.println(interpreted);
		assertEquals(3, count("Hello, World!", interpreted)); 
	}
	
	private int count(String pattern, String source) {
		Pattern regex = Pattern.compile("(.*)"+pattern+".*", Pattern.DOTALL);
		String cur = source;
		Matcher m;
		int count = 0;
		
		while(true) {
			m = regex.matcher(cur);
			if(m.matches()) {
				count ++;
				cur = m.group(1);
			} else {
				return count;
			}
		}
	}
}
