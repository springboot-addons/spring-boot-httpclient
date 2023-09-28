package io.github.springboot.httpclient5.actuator.autoconfigure;

import static com.codahale.metrics.MetricRegistry.name;

import org.apache.hc.client5.http.classic.ExecChainHandler;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.httpclient5.HttpClientMetricNameStrategies;
import com.codahale.metrics.httpclient5.HttpClientMetricNameStrategy;
import com.codahale.metrics.jmx.JmxReporter;

import io.github.springboot.httpclient5.actuator.ActuatorMetricExecChainHandler;
import io.github.springboot.httpclient5.actuator.HttpClientEndpoint;
import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import jakarta.annotation.PreDestroy;

@Configuration
@ConditionalOnProperty(name = "spring.httpclient5.core.actuator.enabled", havingValue = "true", matchIfMissing = true)
public class HttpClientActuatorAutoConfiguration {
    private static final String METRICS_PREFIX = HttpClientConnectionManager.class.getName();
	@Autowired
	protected HttpClient5Config config;

	private String name;
	private MetricRegistry metricRegistry;
	
	@Bean("legacyMetricRegistry")
	public MetricRegistry getMetricsRegistry(PoolingHttpClientConnectionManager cm) {
		metricRegistry = new MetricRegistry();
        // this acquires a lock on the connection pool; remove if contention sucks
        metricRegistry.registerGauge(name(METRICS_PREFIX, name, "available-connections"),
                () -> {
                    return cm.getTotalStats().getAvailable();
                });
        // this acquires a lock on the connection pool; remove if contention sucks
        metricRegistry.registerGauge(name(METRICS_PREFIX, name, "leased-connections"),
                () -> cm.getTotalStats().getLeased());
        // this acquires a lock on the connection pool; remove if contention sucks
        metricRegistry.registerGauge(name(METRICS_PREFIX, name, "max-connections"),
                () -> cm.getTotalStats().getMax()
        );
        // this acquires a lock on the connection pool; remove if contention sucks
        metricRegistry.registerGauge(name(METRICS_PREFIX, name, "pending-connections"),
                () -> cm.getTotalStats().getPending());
		return metricRegistry;
	}

	@PreDestroy
	public void dispose() {
		metricRegistry.remove(name(METRICS_PREFIX, name, "available-connections"));
		metricRegistry.remove(name(METRICS_PREFIX, name, "leased-connections"));
		metricRegistry.remove(name(METRICS_PREFIX, name, "max-connections"));
		metricRegistry.remove(name(METRICS_PREFIX, name, "pending-connections"));
	}
	
	@Bean
	public HttpClientEndpoint httpClientEndpoint(@Qualifier("legacyMetricRegistry") MetricRegistry metricRegistry) {
		return new HttpClientEndpoint(config, metricRegistry);
	}

	@Bean
	public JmxReporter jmxExporter(@Qualifier("legacyMetricRegistry") MetricRegistry metricRegistry) {
		final JmxReporter reporter = JmxReporter.forRegistry(metricRegistry)
				.inDomain(config.getJmx().getDomain()).build();
		reporter.start();
		return reporter;
	}

	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	public ExecChainHandler chainableInstrumentedHttpRequestExecutor(
			@Qualifier("legacyMetricRegistry") MetricRegistry metricRegistry) {
		HttpClientMetricNameStrategy metricNameStrategy = getMetricNameStrategy(config.getJmx().getMetricNameStrategy());
		return new ActuatorMetricExecChainHandler(metricRegistry, metricNameStrategy);
	}

	private HttpClientMetricNameStrategy getMetricNameStrategy(String name) {
		HttpClientMetricNameStrategy nameStrategy = HttpClientMetricNameStrategies.HOST_AND_METHOD;
		if ("QUERYLESS_URL_AND_METHOD".equalsIgnoreCase(name)) {
			nameStrategy = HttpClientMetricNameStrategies.QUERYLESS_URL_AND_METHOD;
		} else if ("METHOD_ONLY".equalsIgnoreCase(name)) {
			nameStrategy = HttpClientMetricNameStrategies.METHOD_ONLY;
		}

		return nameStrategy;
	}
}
