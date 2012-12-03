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
package org.bndtools.rt.repository.server;

import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream extends InputStream {
	
	private final InputStream delegate;
	private final long limit;
	
	private long count = 0;

	public LimitedInputStream(InputStream delegate, long limit) {
		this.delegate = delegate;
		this.limit = limit;
	}
	
	private synchronized void bumpCount(long bump) {
		count += bump;
		if (count > limit)
			throw new IllegalStateException("You read too much data");
	}

	@Override
	public int read() throws IOException {
		int result = delegate.read();
		bumpCount(1);
		return result;
	}

	@Override
	public int read(byte[] b) throws IOException {
		int bytes = delegate.read(b);
		if (bytes > 0)
			bumpCount(bytes);
		return bytes;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int bytes = delegate.read(b, off, len);
		if (bytes > 0)
			bumpCount(bytes);
		return bytes;
	}

	@Override
	public long skip(long n) throws IOException {
		long skipped = delegate.skip(n);
		bumpCount(skipped);
		return skipped;
	}

	@Override
	public int available() throws IOException {
		return delegate.available();
	}

	@Override
	public void close() throws IOException {
		delegate.close();
	}

	@Override
	public void mark(int readlimit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void reset() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean markSupported() {
		return false;
	}
	

}
