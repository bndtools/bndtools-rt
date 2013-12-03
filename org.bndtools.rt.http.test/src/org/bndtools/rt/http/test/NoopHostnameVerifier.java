package org.bndtools.rt.http.test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class NoopHostnameVerifier implements HostnameVerifier {

	@Override
	public boolean verify(String hostname, SSLSession session) {
		System.out.println("Verifying hostname: " + hostname);
		return true;
	}

}
