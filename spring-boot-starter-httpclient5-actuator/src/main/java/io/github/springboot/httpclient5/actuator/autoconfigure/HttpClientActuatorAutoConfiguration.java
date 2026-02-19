package io.github.springboot.httpclient5.actuator.autoconfigure;

import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import io.micrometer.core.instrument.binder.httpcomponents.hc5.ObservationExecChainHandler;
import io.micrometer.core.instrument.binder.httpcomponents.hc5.PoolingHttpClientConnectionManagerMetricsBinder;
import io.micrometer.observation.ObservationRegistry;

@Configuration
@ConditionalOnProperty(name = "spring.httpclient5.core.actuator.enabled", havingValue = "true", matchIfMissing = true)
public class HttpClientActuatorAutoConfiguration {

	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	public ObservationExecChainHandler observationExecChainHandler(ObservationRegistry observationRegistry) {
		return new ObservationExecChainHandler(observationRegistry);
	}
	
	@Bean
	PoolingHttpClientConnectionManagerMetricsBinder poolingHttpClientConnectionManagerMetricsBinder(PoolingHttpClientConnectionManager cm) {
		return new PoolingHttpClientConnectionManagerMetricsBinder(cm, "httpclient5.pool");
	}

	@Bean
	PoolingHttpClientConnectionManagerMetricsBinder asyncPoolingHttpClientConnectionManagerMetricsBinder(PoolingAsyncClientConnectionManager asyncCm) {
		return new PoolingHttpClientConnectionManagerMetricsBinder(asyncCm, "httpclient5.async-pool");
	}
}
