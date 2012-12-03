/*******************************************************************************
 * Copyright (c) 2012 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package org.bndtools.rt.repository.rest.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;

import junit.framework.TestCase;

import org.bndtools.rt.repository.client.RemoteRestRepository;

import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.PutResult;
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
	private RemoteRestRepository readRepo;
	private RemoteRestRepository writeRepo;
	
	@Override
	protected void setUp() throws Exception {
		cacheDir = new File("generated/cache");
		IO.deleteWithException(cacheDir);
		cacheDir.mkdirs();
		if (!cacheDir.isDirectory())
			throw new Exception("Failed to create cache dir: " + cacheDir);
		
		Map<String, String> configProps;
		
		// Configure the readable repo
		configProps = new HashMap<String, String>();
		configProps.put("url", "http://localhost:8080/testrepo");
		configProps.put("cache", cacheDir.getAbsolutePath());
		readRepo = new RemoteRestRepository();
		readRepo.setProperties(configProps);
		
		// Clean and recreate the repodir for the writeable repo
		File repodir = new File("generated/repodir1");
		IO.deleteWithException(repodir);
		repodir.mkdirs();
		if (!repodir.isDirectory())
			throw new Exception("Failed to create repo dir: " + repodir);
		
		// Configure the writeable repo
		configProps = new HashMap<String, String>();
		configProps.put("url", "http://localhost:8080/repo1");
		configProps.put("cache", cacheDir.getAbsolutePath());
		writeRepo = new RemoteRestRepository();
		writeRepo.setProperties(configProps);
	}
	
	public void testList() throws Exception {
		List<String> list = readRepo.list("org.*");
		assertEquals(1, list.size());
		assertEquals("org.example.foo", list.get(0));
	}
	
	public void testVersions() throws Exception {
		SortedSet<Version> versions = readRepo.versions("org.example.foo");
		assertEquals(1, versions.size());
		assertEquals("1.2.3.qualifier", versions.iterator().next().toString());
	}
	
	public void testGetBundleExact() throws Exception {
		File downloadedFile = readRepo.get("org.example.foo", new Version("1.2.3.qualifier"), null);
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
	
	public void testPutBundle() throws Exception {
		InputStream bundleStream = ClientPluginTest.class.getResourceAsStream("com.example.1.jar");
		PutResult result = writeRepo.put(bundleStream, RepositoryPlugin.DEFAULTOPTIONS);
		assertEquals("http://localhost:8080/repo1/bundles/com.example.1/1.0.0", result.artifact.toString());
	}
}
