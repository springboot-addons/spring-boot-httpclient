package io.github.springboot.httpclient;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public interface HttpClientConnectionManagerCustomizer {
	
	public void customize(PoolingHttpClientConnectionManager cm) ;

}
