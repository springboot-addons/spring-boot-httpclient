package io.github.springboot.httpclient5.core.configure;

public interface PoolingHttpClientConnectionManagerPostConfigurer {
	
	public void configure(ConfigurableConnPoolControl cm) ;
}
