package org.bndtools.rt.repository.rest.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import junit.framework.TestCase;

import org.bndtools.rt.repository.client.RemoteRestRepository;

import aQute.bnd.version.Version;

public class ClientPluginTest extends TestCase {
	
	static {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		}
	}

	private RemoteRestRepository repoPlugin;
	
	@Override
	protected void setUp() throws Exception {
		repoPlugin = new RemoteRestRepository();
		Map<String, String> configProps = new HashMap<String, String>();
		configProps.put("url", "http://localhost:8080/testrepo");
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
}
