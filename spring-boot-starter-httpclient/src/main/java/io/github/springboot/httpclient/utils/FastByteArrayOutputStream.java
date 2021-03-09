package io.github.springboot.httpclient.utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * ByteArrayOutputStream implementation that doesn't synchronize methods and
 * doesn't copy the data on toByteArray().
 */
public class FastByteArrayOutputStream extends OutputStream {

	/**
	 * Buffer and size.
	 */
	protected byte[] buf; // null

	protected int size; // 0

	/**
	 * Constructs a stream with buffer capacity size 5K.
	 */
	public FastByteArrayOutputStream() {
		this(5 * 1024);
	}

	/**
	 * Constructs a stream with the given initial size.
	 */
	public FastByteArrayOutputStream(int initSize) {
		this.size = 0;
		this.buf = new byte[initSize];
	}

	/**
	 * Ensures that we have a large enough buffer for the given size.
	 */
	private void verifyBufferSize(int sz) {
		if (sz > buf.length) {
			byte[] old = buf;
			buf = new byte[Math.max(sz, 2 * buf.length)];
			System.arraycopy(old, 0, buf, 0, old.length);
			old = null;
		}
	}

	public int getSize() {
		return size;
	}

	/**
	 * Returns the byte array containing the written data. Note that this array will
	 * almost always be larger than the amount of data actually written.
	 */
	public byte[] getByteArray() {
		return buf;
	}

	/**
	 * Returns the byte array containing the written data. Note that this array will
	 * almost always be larger than the amount of data actually written.
	 */
	public byte[] toByteArray() {
		final byte[] res = new byte[getSize()];
		System.arraycopy(buf, 0, res, 0, size);
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void write(byte[] b) {
		verifyBufferSize(size + b.length);
		System.arraycopy(b, 0, buf, size, b.length);
		size += b.length;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void write(byte[] b, int off, int len) {
		verifyBufferSize(size + len);
		System.arraycopy(b, off, buf, size, len);
		size += len;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void write(int b) {
		verifyBufferSize(size + 1);
		buf[size++] = (byte) b;
	}

	public void reset() {
		size = 0;
	}

	public void setLength(int newLength) {
		size = newLength;
	}

	/**
	 * Returns a ByteArrayInputStream for reading back the written data.
	 */
	public InputStream getInputStream() {
		return new FastByteArrayInputStream(buf, size);
	}

	public String toString(String charset) throws UnsupportedEncodingException {
		return new String(buf, 0, size, charset);
	}

	public String toString(Charset charset) {
		return new String(buf, 0, size, charset);
	}

}
