package org.bndtools.rt.http.test;

import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

public final class NoopTrustManager implements X509TrustManager {

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
		// no-op
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
		System.out.println("CHECKING SERVER TRUST....");
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return new X509Certificate[0];
	}

}