package org.springframework.boot.httpclient;

import java.util.List;

import javax.inject.Provider;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.fluent.Executor;
import org.apache.http.config.Registry;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.ConfigurableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.httpclient.auth.CookieProcessingTargetAuthenticationStrategy;
import org.springframework.boot.httpclient.config.HttpClientConfigurationHelper;
import org.springframework.boot.httpclient.constants.ConfigurationConstants;
import org.springframework.boot.httpclient.interceptors.HttpInterceptorsProviderConfig.HttpInterceptorsProvider;
import org.springframework.boot.httpclient.internal.RequestConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.httpclient.HttpClientMetricNameStrategies;
import com.codahale.metrics.httpclient.HttpClientMetricNameStrategy;
import com.codahale.metrics.httpclient.InstrumentedHttpRequestExecutor;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

/**
 * @author srouthiau
 *
 */
@Configuration
@ComponentScan("org.springframework.boot.httpclient")
public class HttpClientProvider {

  @Autowired(required = false)
  private Provider<CookieStore> cookieStore;

  @Autowired
  private HttpInterceptorsProvider interceptors;

  @Autowired
  private RequestConfigurer configurer;

  @Autowired
  private HttpClientConfigurationHelper config;

  @Autowired
  private Registry<AuthSchemeProvider> authSchemeRegistry;

  @Autowired
  private RequestConfig defaultRequestConfig;

  @Autowired
  private CredentialsProvider credentialsProvider;

  @Autowired
  private HttpClientConnectionManager connectionManager;

  @Autowired
  @Qualifier("legacyMetricRegistry")
  private MetricRegistry metricRegistry;

  @Autowired
  private CircuitBreakerRegistry cbRegistry;

  @Autowired
  private RateLimiterRegistry rlRegistry;

  @Bean
  public Executor httpClientExecutor(HttpClient httpClient) {
    return Executor.newInstance(httpClient);
  }

  @Bean
  public CloseableHttpClient httpClient() {
    // Create HttpClient
    final HttpClientBuilder clientBuilder = HttpClientBuilder.create();
    final HttpClientMetricNameStrategy metricNameStrategy = getMetricNameStrategy(
        config.getGlobalConfiguration(ConfigurationConstants.METRIC_NANE_STRATEGY));
    clientBuilder.setRequestExecutor(new InstrumentedHttpRequestExecutor(metricRegistry, metricNameStrategy));
    clientBuilder.setConnectionManager(connectionManager);
    clientBuilder.setDefaultAuthSchemeRegistry(authSchemeRegistry);
    clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
    clientBuilder.setDefaultRequestConfig(defaultRequestConfig);
    clientBuilder.setTargetAuthenticationStrategy(CookieProcessingTargetAuthenticationStrategy.INSTANCE);

    clientBuilder.setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE);
    clientBuilder.setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE);

    final String userAgent = config.getGlobalConfiguration(ConfigurationConstants.USER_AGENT);
    clientBuilder.setUserAgent(userAgent);

    final List<HttpRequestInterceptor> requestInterceptors = interceptors.getRequestInterceptors();
    if (requestInterceptors != null && !requestInterceptors.isEmpty()) {
      for (final HttpRequestInterceptor interceptor : requestInterceptors) {
        clientBuilder.addInterceptorLast(interceptor);
      }
    }
    final List<HttpResponseInterceptor> responseInterceptors = interceptors.getResponseInterceptors();
    if (responseInterceptors != null && !responseInterceptors.isEmpty()) {
      for (final HttpResponseInterceptor interceptor : responseInterceptors) {
        clientBuilder.addInterceptorLast(interceptor);
      }
    }

    final List<HttpRequestInterceptor> firstRequestInterceptors = interceptors.getFirstRequestInterceptors();
    if (firstRequestInterceptors != null && !firstRequestInterceptors.isEmpty()) {
      for (final HttpRequestInterceptor interceptor : requestInterceptors) {
        clientBuilder.addInterceptorFirst(interceptor);
      }
    }

    final List<HttpResponseInterceptor> firstResponseInterceptors = interceptors.getFirstResponseInterceptors();
    if (firstResponseInterceptors != null && !firstResponseInterceptors.isEmpty()) {
      for (final HttpResponseInterceptor interceptor : firstResponseInterceptors) {
        clientBuilder.addInterceptorFirst(interceptor);
      }
    }

    if (config.isCookieManagementDisabled() || cookieStore.get() == null) {
      clientBuilder.disableCookieManagement();
    } else {
      clientBuilder.setDefaultCookieStore(cookieStore.get());
    }

    return new ConfigurableHttpClient(config, cbRegistry, rlRegistry, clientBuilder.build(), configurer);
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
