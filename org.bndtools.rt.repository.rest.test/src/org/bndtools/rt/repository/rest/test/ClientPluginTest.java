package org.bndtools.rt.repository.rest.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;

import junit.framework.TestCase;

import org.bndtools.rt.repository.client.RemoteRestRepository;

import aQute.bnd.version.Version;
import aQute.lib.io.IO;

public class ClientPluginTest extends TestCase {
	
	static {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		}
	}

	private File cacheDir;
	private RemoteRestRepository repoPlugin;
	
	@Override
	protected void setUp() throws Exception {
		cacheDir = new File("generated/cache");
		IO.deleteWithException(cacheDir);
		cacheDir.mkdirs();
		if (!cacheDir.isDirectory())
			throw new Exception("Failed to create cache dir");
		
		Map<String, String> configProps = new HashMap<String, String>();
		configProps.put("url", "http://localhost:8080/testrepo");
		configProps.put("cache", cacheDir.getAbsolutePath());
		
		repoPlugin = new RemoteRestRepository();
		repoPlugin.setProperties(configProps);
	}
	
	public void testList() throws Exception {
		List<String> list = repoPlugin.list("org.*");
		assertEquals(1, list.size());
		assertEquals("org.example.foo", list.get(0));
	}
	
	public void testVersions() throws Exception {
		SortedSet<Version> versions = repoPlugin.versions("org.example.foo");
		assertEquals(1, versions.size());
		assertEquals("1.2.3.qualifier", versions.iterator().next().toString());
	}
	
	public void testGetBundleExact() throws Exception {
		File downloadedFile = repoPlugin.get("org.example.foo", new Version("1.2.3.qualifier"), null);
		assertEquals(new File("generated/cache", "http%3A%2F%2Flocalhost%3A8080%2Ftestrepo%2Fbundles%2Forg.example.foo/1.2.3.qualifier").getAbsoluteFile(), downloadedFile);
		JarInputStream jarStream = new JarInputStream(new FileInputStream(downloadedFile));
		try {
			Attributes attribs = jarStream.getManifest().getMainAttributes();
			assertEquals("org.example.foo", attribs.getValue("Bundle-SymbolicName"));
			assertEquals("1.2.3.qualifier", attribs.getValue("Bundle-Version"));
		} finally {
			jarStream.close();
		}
	}
}
