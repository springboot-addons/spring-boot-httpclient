package io.github.springboot.httpclient5.core.configure.async;

import org.apache.hc.client5.http.SchemePortResolver;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.AsyncConnectionManagerConfigProperties;
import io.github.springboot.httpclient5.core.configure.ConfigurableConnPoolControl;
import io.github.springboot.httpclient5.core.configure.PoolingHttpClientConnectionManagerPostConfigurer;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class HttpAsyncClientConnectionManagerConfigurer {
	@Autowired
	private HttpClient5Config config;
	
	@Autowired
	private ObjectProvider<PoolingHttpClientConnectionManagerPostConfigurer> cmConfigurers;
	
	@Bean
	public PoolingAsyncClientConnectionManager defaultAsyncConnectionManager(
//			@Autowired(required = false) ObjectProvider<LayeredConnectionSocketFactory> sslSocketFactoryProvider,
//			@Autowired(required = false) ObjectProvider<HttpConnectionFactory<ManagedHttpClientConnection>> httpConnectionFactory,
			@Autowired(required = false) ObjectProvider<SchemePortResolver> schemePortResolverProvider
			) {
        // Create a connection manager with custom configuration.
		AsyncConnectionManagerConfigProperties pool = config.getAsyncPool();
		// Unavailable
		//httpConnectionFactory.ifAvailable(pool::setConnectionFactory);
		// Unavailable
		//sslSocketFactoryProvider.ifAvailable(pool::setSSLSocketFactory);
		schemePortResolverProvider.ifAvailable(pool::setSchemePortResolver);
		
		// Unavailable
		//pool.setDefaultSocketConfig(pool.getDefaultSocketConfig().build()) ;
		if (pool.getDefaultConnectionConfig() != null) {
			pool.setDefaultConnectionConfig(pool.getDefaultConnectionConfig().build()) ;
		}
		log.debug("Connection Manager is {}", pool);
		
		PoolingAsyncClientConnectionManager connectionManager = pool.build();
		ConfigurableConnPoolControl wrapper = new PoolingAsyncClientConnectionManagerWrapper(connectionManager) ;
		cmConfigurers.orderedStream().forEach(c -> c.configure(wrapper, true));
		
		return connectionManager ;
	}
	
	@RequiredArgsConstructor
	public static class PoolingAsyncClientConnectionManagerWrapper implements ConfigurableConnPoolControl {
		@Delegate
		private final PoolingAsyncClientConnectionManager internal ;
	}
}
