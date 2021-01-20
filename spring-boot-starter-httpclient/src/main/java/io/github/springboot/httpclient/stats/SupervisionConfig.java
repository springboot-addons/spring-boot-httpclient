package io.github.springboot.httpclient.stats;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;

import io.github.springboot.httpclient.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.constants.ConfigurationConstants;
import io.github.springboot.httpclient.stats.actuator.HttpClientEndpoint;

@Configuration
public class SupervisionConfig {

  @Autowired
  protected HttpClientConfigurationHelper config;

  @Autowired
  @Qualifier("legacyMetricRegistry")
  private MetricRegistry metricRegistry;

  public SupervisionConfig() {
  }

  @Bean
  public HttpClientEndpoint httpClientEndpoint() {
    return new HttpClientEndpoint(config.getAllConfigurations(), metricRegistry);
  }

  @Bean
  public JmxReporter jmxExporter() {
    final JmxReporter reporter = JmxReporter.forRegistry(metricRegistry)
        .inDomain(config.getGlobalConfiguration(ConfigurationConstants.JMX_DOMAIN)).build();
    reporter.start();
    return reporter;
  }
}
