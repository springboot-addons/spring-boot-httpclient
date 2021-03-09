package io.github.springboot.httpclient.actuator.autoconfigure;

import org.apache.http.config.Registry;
import org.apache.http.conn.HttpClientConnectionOperator;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.httpclient.HttpClientMetricNameStrategies;
import com.codahale.metrics.httpclient.HttpClientMetricNameStrategy;
import com.codahale.metrics.httpclient.InstrumentedHttpClientConnectionManager;
import com.codahale.metrics.jmx.JmxReporter;

import io.github.springboot.httpclient.actuator.ChainableInstrumentedHttpRequestExecutor;
import io.github.springboot.httpclient.actuator.HttpClientEndpoint;
import io.github.springboot.httpclient.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.constants.ConfigurationConstants;
import io.github.springboot.httpclient.internal.ChainableHttpRequestExecutor;

@Configuration
@ConditionalOnProperty(name = "httpclient.core.actuator.enabled", havingValue = "true", matchIfMissing = true)
public class HttpClientActuatorAutoConfiguration {

	@Autowired
	protected HttpClientConfigurationHelper config;

	@Autowired
	private HttpClientConnectionOperator httpClientConnectionOperator;

	@Bean("legacyMetricRegistry")
	public MetricRegistry getMetricsRegistry() {
		return new MetricRegistry();
	}

	@Bean
	public HttpClientEndpoint httpClientEndpoint(@Qualifier("legacyMetricRegistry") MetricRegistry metricRegistry) {
		return new HttpClientEndpoint(config.getAllConfigurations(), metricRegistry);
	}

	@Bean
	public JmxReporter jmxExporter(@Qualifier("legacyMetricRegistry") MetricRegistry metricRegistry) {
		final JmxReporter reporter = JmxReporter.forRegistry(metricRegistry)
				.inDomain(config.getGlobalConfiguration(ConfigurationConstants.JMX_DOMAIN)).build();
		reporter.start();
		return reporter;
	}

	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	public ChainableHttpRequestExecutor chainableInstrumentedHttpRequestExecutor(
			@Qualifier("legacyMetricRegistry") MetricRegistry metricRegistry) {
		HttpClientMetricNameStrategy metricNameStrategy = getMetricNameStrategy(
				config.getGlobalConfiguration(ConfigurationConstants.METRIC_NANE_STRATEGY));
		return new ChainableInstrumentedHttpRequestExecutor(metricRegistry, metricNameStrategy);
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

	@Bean
	@Primary
	public PoolingHttpClientConnectionManager instrumentedConnectionManager(Registry<ConnectionSocketFactory> registry,
			@Qualifier("legacyMetricRegistry") MetricRegistry metricRegistry) {
		return InstrumentedHttpClientConnectionManager.builder(metricRegistry)
				.httpClientConnectionOperator(httpClientConnectionOperator).socketFactoryRegistry(registry).build();
	}
}
