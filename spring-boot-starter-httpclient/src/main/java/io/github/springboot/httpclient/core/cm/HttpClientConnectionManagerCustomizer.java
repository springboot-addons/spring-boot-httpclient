package io.github.springboot.httpclient.core.cm;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public interface HttpClientConnectionManagerCustomizer {

	public void customize(PoolingHttpClientConnectionManager cm);

}
