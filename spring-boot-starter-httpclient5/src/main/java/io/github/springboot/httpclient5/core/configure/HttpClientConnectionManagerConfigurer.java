package io.github.springboot.httpclient5.core.configure;

import org.apache.hc.client5.http.SchemePortResolver;
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.ManagedHttpClientConnection;
import org.apache.hc.client5.http.socket.LayeredConnectionSocketFactory;
import org.apache.hc.core5.http.io.HttpConnectionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.ConnectionManagerConfigProperties;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class HttpClientConnectionManagerConfigurer {
	@Autowired
	private HttpClient5Config config;
	
	@Autowired
	private ObjectProvider<PoolingHttpClientConnectionManagerPostConfigurer> cmConfigurers;
	
	@Bean
	public PoolingHttpClientConnectionManager defaultConnectionManager(
			@Autowired(required = false) ObjectProvider<LayeredConnectionSocketFactory> sslSocketFactoryProvider,
			@Autowired(required = false) ObjectProvider<HttpConnectionFactory<ManagedHttpClientConnection>> httpConnectionFactory,
			@Autowired(required = false) ObjectProvider<SchemePortResolver> schemePortResolverProvider
			) {
        // Create a connection manager with custom configuration.
		ConnectionManagerConfigProperties pool = config.getPool();
		httpConnectionFactory.ifAvailable(pool::setConnectionFactory);
		sslSocketFactoryProvider.ifAvailable(pool::setSSLSocketFactory);
		schemePortResolverProvider.ifAvailable(pool::setSchemePortResolver);
		pool.setDefaultSocketConfig(pool.getSocketConfig().build()) ;
		log.debug("Connection Manager is {}", pool);
		
		PoolingHttpClientConnectionManager connectionManager = pool.build();
		
		cmConfigurers.orderedStream().forEach(c -> c.configure(connectionManager));
		
		return connectionManager ;
	}
	
	@Bean
	@ConditionalOnMissingBean
	public SchemePortResolver schemePortResolver() {
		return new DefaultSchemePortResolver();
	}
}
