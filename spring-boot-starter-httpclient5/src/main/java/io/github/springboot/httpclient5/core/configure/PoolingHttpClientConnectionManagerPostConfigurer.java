package io.github.springboot.httpclient5.core.configure;

import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;

public interface PoolingHttpClientConnectionManagerPostConfigurer {
	
	public void configure(PoolingHttpClientConnectionManager cm) ;
}
