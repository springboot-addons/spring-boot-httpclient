package io.github.springboot.httpclient.utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * ByteArrayInputStream implementation that does not synchronize methods.
 */
public class FastByteArrayInputStream extends InputStream {

  /**
   * Our byte buffer.
   */
  protected byte[] buf;

  /**
   * Number of bytes that we can read from the buffer.
   */
  protected int count; // 0

  /**
   * Number of bytes that have been read from the buffer.
   */
  protected int pos; // 0

  public FastByteArrayInputStream(byte[] buf) {
    this(buf, buf.length);
  }

  public FastByteArrayInputStream(byte[] buf, int count) {
    this.buf = buf;
    this.count = count;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final int available() {
    return count - pos;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final int read() {
    return pos < count ? buf[pos++] & 0xff : -1;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final int read(byte[] b, int off, int len) {
    if (pos >= count) {
      return -1;
    }

    if (pos + len > count) {
      len = count - pos;
    }

    System.arraycopy(buf, pos, b, off, len);
    pos += len;
    return len;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final long skip(long n) {
    if (pos + n > count) {
      n = count - pos;
    }
    if (n < 0) {
      return 0;
    }
    pos += n;
    return n;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() throws IOException {
    pos = 0;
  }

}
