package io.github.springboot.httpclient.internal;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;

public class ProgressEntityWrapper extends HttpEntityWrapper {
  private final ProgressListener listener;

  public ProgressEntityWrapper(HttpEntity entity, ProgressListener listener) {
    super(entity);
    this.listener = listener;
  }

  @Override
  public void writeTo(OutputStream outstream) throws IOException {
    super.writeTo(new CountingOutputStream(outstream, listener, getContentLength()));
  }

  public static interface ProgressListener {
    void progress(float percentage);
  }

  public static class CountingOutputStream extends FilterOutputStream {
    private final ProgressListener listener;
    private long transferred;
    private final long totalBytes;

    public CountingOutputStream(OutputStream out, ProgressListener listener, long totalBytes) {
      super(out);
      this.listener = listener;
      transferred = 0;
      this.totalBytes = totalBytes;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      out.write(b, off, len);
      transferred += len;
      listener.progress(getCurrentProgress());
    }

    @Override
    public void write(int b) throws IOException {
      out.write(b);
      transferred++;
      listener.progress(getCurrentProgress());
    }

    private float getCurrentProgress() {
      return (float) transferred / totalBytes * 100;
    }
  }
}