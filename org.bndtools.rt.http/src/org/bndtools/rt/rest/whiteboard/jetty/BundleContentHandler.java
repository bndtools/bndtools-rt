package org.bndtools.rt.rest.whiteboard.jetty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.osgi.framework.Bundle;

import aQute.lib.io.IO;

public class BundleContentHandler extends AbstractHandler {
	
	private static class BundlePath {
		Bundle bundle; String prefix;
	}
	
	private final Map<String, BundlePath> pathMap = new HashMap<String, BundlePath>();
	
	public void addAlias(String alias, Bundle bundle, String prefix) {
		alias = normalize(alias);
		prefix = normalize(prefix);

		BundlePath path = new BundlePath();
		path.bundle = bundle;
		path.prefix = prefix;

		synchronized (pathMap) {
			pathMap.put(alias, path);
		}
	}

	public void removeAlias(String alias) {
		synchronized (pathMap) {
			pathMap.remove(normalize(alias));
		}
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		Bundle bundle = null;
		String path = null;

		synchronized (pathMap) {
			for (Entry<String, BundlePath> entry : pathMap.entrySet()) {
				String alias = entry.getKey();
				if (target.startsWith(alias)) {
					bundle = entry.getValue().bundle;
					path = entry.getValue().prefix + target.substring(alias.length());
					break;
				}
			}
		}

		if (bundle != null && path != null) {
			URL entry = bundle.getEntry(path);
			if (entry != null) {
				OutputStream out = response.getOutputStream();
				try {
					InputStream in = entry.openStream();
					IO.copy(in, out);
				} finally {
					IO.close(out);
				}
			}
		}
	}
	
	
	private static String normalize(String prefix) {
		assert prefix != null;
		return prefix.endsWith("/") ? prefix : prefix + "/";
	}

}
