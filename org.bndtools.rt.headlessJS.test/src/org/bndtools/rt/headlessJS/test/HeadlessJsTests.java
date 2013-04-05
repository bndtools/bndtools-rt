package org.bndtools.rt.headlessJS.test;

import javax.servlet.http.HttpServlet;

import junit.framework.TestCase;

public class HeadlessJsTests extends TestCase {

	String jsPage = "<html><body><script type='text/javascript'>document.write('Hello, world!');</script></body></html>";
	String interpretedPage = "<html><body>Hello, world!</body></html>";
	
}
