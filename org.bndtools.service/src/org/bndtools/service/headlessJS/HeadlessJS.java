package org.bndtools.service.headlessJS;

import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Service returning a URI from which a version
 * of the requested page with pre-interpreted
 * javascript can be retrieved.
 * 
 */
public interface HeadlessJS {

	/**
	 * Provides a URI from which a version
	 * of the requested page with pre-interpreted
	 * javascript can be retrieved.
	 * 
	 * @param page Page with javascript to be pre-interpreted
	 * @return an URI providing an interpreted version of the page
	 * @throws URISyntaxException 
	 * @throws Exception 
	 */
	URI getInterpretedPage(URI page) throws Exception;
	void getInterpretedPage(URI page, OutputStream out) throws Exception;
}