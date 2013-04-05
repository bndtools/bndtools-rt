package org.bndtools.rt.headlessJS.phantomJS;


import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import org.bndtools.service.headlessJS.HeadlessJS;
import org.bndtools.service.packager.ProcessGuard;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import aQute.lib.io.IO;

/**
 * Provides the {@link HeadlessJS} service. In order to work,
 * this bundle needs a reference to a phantomjs process guard,
 * which should ensure that there is a headless javascript 
 * interpreter running in localhost, behind the port specified
 * in the corresponding process guard configuration.
 * 
 */
@Component(	provide = HeadlessJS.class )
public class PhantomJS implements HeadlessJS {

	int port;
	String host = "localhost";
	
	@Reference(target="(type=phantomjs)")
	public void setSnapshotServerGuard(ProcessGuard snapshotServerGuard) {
		port = Integer.parseInt((String) snapshotServerGuard.getProperties().get("port"));
	}
	
	@Override
	public URI getInterpretedPage(URI page) throws Exception {
		return new URI("http://"+host+":"+port+"/"+page);
	}

	@Override
	public void getInterpretedPage(URI page, OutputStream out) throws Exception {
		URI interpretedPage = getInterpretedPage(page);
		HttpURLConnection connection = (HttpURLConnection) interpretedPage
				.toURL().openConnection();
		IO.copy(connection.getInputStream(), out);
		connection.disconnect();
	}

}