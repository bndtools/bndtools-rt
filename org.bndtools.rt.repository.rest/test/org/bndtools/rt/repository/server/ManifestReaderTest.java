package org.bndtools.rt.repository.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;

import junit.framework.TestCase;

public class ManifestReaderTest extends TestCase {
	
	public void testManifestReader() throws Exception {
		File tmp = File.createTempFile("big", ".jar");
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("MyHeader", "foo");
		createBigJar(tmp, headers);
		System.out.printf("Created with length %d: %s%n", tmp.length(), tmp.getAbsolutePath());
		
		LimitedInputStream ltdInput = new LimitedInputStream(new FileInputStream(tmp), 100000);
		BufferedInputStream buffInput = new BufferedInputStream(ltdInput, 10000);
		
		buffInput.mark(9999);
		readManifest(buffInput);
		buffInput.reset();
		readManifest(buffInput);
		
		/*
		try {
			byte[] buf = new byte[2048];
			while (true) {
				int read = ltdInput.read(buf, 0, 2048);
				if (read == -1) break;
			}
		} finally {
			ltdInput.close();
		}
		*/
		
	}

	private void readManifest(InputStream buffInput) throws IOException {
		JarInputStream jarInput = new JarInputStream(buffInput);
		Manifest manifest = jarInput.getManifest();
		Attributes attribs = manifest.getMainAttributes();
		for (Entry<Object,Object> entry : attribs.entrySet()) {
			System.out.printf("Entry %s = %s%n", entry.getKey(), entry.getValue());
		}
	}

	void createBigJar(File tmp, Map<String, String> headers) throws IOException {
		Manifest manifest = new Manifest();
		Attributes attribs = manifest.getMainAttributes();
		attribs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		for (Entry<String, String> entry : headers.entrySet()) {
			attribs.putValue(entry.getKey(), entry.getValue());
		}
		
		FileOutputStream stream = new FileOutputStream(tmp);
		try {
			JarOutputStream zipStream = new JarOutputStream(stream, manifest);
			zipStream.setLevel(Deflater.NO_COMPRESSION);
			
			for (int i = 0; i < 100; i++) {
				String name = String.format("name%03d", i);
				zipStream.putNextEntry(new ZipEntry(name));
				
				for (int j = 0 ; j < 100 ; j++) {
					byte[] buf = new byte[1024];
					Arrays.fill(buf, (byte) 0xFF);
					zipStream.write(buf);
				}
			}
			zipStream.close();
		} finally {
			stream.close();
		}
	}
	
}
