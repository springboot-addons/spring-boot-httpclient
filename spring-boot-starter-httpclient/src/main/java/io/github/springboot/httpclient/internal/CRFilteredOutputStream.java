package io.github.springboot.httpclient.internal;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CRFilteredOutputStream extends FilterOutputStream {

	/**
	 *
	 * @param out
	 */
	public CRFilteredOutputStream(OutputStream out) {
		super(out);
	}

	/**
	 *
	 * @see java.io.FilterOutputStream#write(int)
	 */
	@Override
	public void write(int b) throws IOException {
		if (b != 13 && b != 10) {
			super.write(b);
		}
	}
}
