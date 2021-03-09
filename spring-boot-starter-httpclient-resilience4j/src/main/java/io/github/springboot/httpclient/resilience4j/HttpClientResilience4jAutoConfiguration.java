package io.github.springboot.httpclient.resilience4j;

import org.apache.http.config.Registry;
import org.apache.http.conn.HttpClientConnectionOperator;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.springboot.httpclient.core.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.core.internal.ChainableHttpRequestExecutor;

@Configuration
@ConditionalOnProperty(name = "httpclient.core.resilience4j.enabled", havingValue = "true", matchIfMissing = true)
public class HttpClientResilience4jAutoConfiguration {
	protected static final String DEFAULT_CIRCUIT = "default";

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public ChainableHttpRequestExecutor resilienceHttpRequestExecutor(HttpClientConfigurationHelper config,
			CircuitBreakerRegistry cbRegistry, RateLimiterRegistry rlRegistry) {
		return new ResilienceHttpRequestExecutor(config, cbRegistry, rlRegistry);
	}

	@Bean
	@Primary
	public HttpClientConnectionOperator instrumentedHttpClientConnectionOperator(HttpClientConfigurationHelper config,
			CircuitBreakerRegistry cbRegistry, Registry<ConnectionSocketFactory> registry) {
		return new Resilience4JHttpClientConnectionOperator(config, cbRegistry, registry);
	}
}