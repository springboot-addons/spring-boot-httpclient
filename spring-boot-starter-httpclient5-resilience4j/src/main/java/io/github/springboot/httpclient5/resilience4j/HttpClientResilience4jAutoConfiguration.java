package io.github.springboot.httpclient5.resilience4j;

import org.apache.hc.client5.http.classic.ExecChainHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.springboot.httpclient5.core.config.HttpClient5Config;

@Configuration
@ConditionalOnProperty(name = "spring.httpclient5.core.resilience4j.enabled", havingValue = "true", matchIfMissing = true)
public class HttpClientResilience4jAutoConfiguration {
	protected static final String DEFAULT_CIRCUIT = "default";

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public ExecChainHandler resilienceHttpRequestExecutor(HttpClient5Config config,
			CircuitBreakerRegistry cbRegistry, RateLimiterRegistry rlRegistry) {
		return new ResilienceExecChainHandler(config, cbRegistry, rlRegistry);
	}
}