package io.github.springboot.httpclient;

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
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import io.github.springboot.httpclient.auth.CookieProcessingTargetAuthenticationStrategy;
import io.github.springboot.httpclient.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.constants.ConfigurationConstants;
import io.github.springboot.httpclient.internal.ChainableHttpRequestExecutor;
import io.github.springboot.httpclient.internal.HttpRequestExecutorChain;
import lombok.extern.slf4j.Slf4j;

/**
 * @author srouthiau
 *
 */
@Configuration
@ComponentScan("io.github.springboot.httpclient")
@Slf4j
public class HttpClientProvider {

  @Autowired(required = false)
  private Provider<CookieStore> cookieStore;

  @Autowired
  private ObjectProvider<ChainableHttpRequestExecutor> chainableHttpRequestExecutors ;

  @Autowired
  private ObjectProvider<HttpRequestInterceptor> requestInterceptors ;

  
  @Autowired
  private ObjectProvider<HttpResponseInterceptor> responseInterceptors ;

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

  @Bean
  public Executor httpClientExecutor(HttpClient httpClient) {
    return Executor.newInstance(httpClient);
  }

  @Bean
  // TODO CloseableHttpClientCustomizer for metrics InstrumentedHttpRequestExecutor
  public CloseableHttpClient httpClient() {
    // Create HttpClient
    final HttpClientBuilder clientBuilder = HttpClientBuilder.create();
    clientBuilder.setRequestExecutor(new HttpRequestExecutorChain(chainableHttpRequestExecutors));
    clientBuilder.setConnectionManager(connectionManager);
    clientBuilder.setDefaultAuthSchemeRegistry(authSchemeRegistry);
    clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
    clientBuilder.setDefaultRequestConfig(defaultRequestConfig);
    clientBuilder.setTargetAuthenticationStrategy(CookieProcessingTargetAuthenticationStrategy.INSTANCE);

    clientBuilder.setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE);
    clientBuilder.setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE);

    final String userAgent = config.getGlobalConfiguration(ConfigurationConstants.USER_AGENT);
    clientBuilder.setUserAgent(userAgent);

    requestInterceptors.forEach(clientBuilder::addInterceptorLast);
    responseInterceptors.forEach(clientBuilder::addInterceptorLast);

    if (config.isCookieManagementDisabled() || cookieStore.get() == null) {
      clientBuilder.disableCookieManagement();
    } else {
      clientBuilder.setDefaultCookieStore(cookieStore.get());
    }

    return clientBuilder.build();
  }
}
