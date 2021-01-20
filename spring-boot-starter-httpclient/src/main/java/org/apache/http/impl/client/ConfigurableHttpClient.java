package org.apache.http.impl.client;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.Retry.Context;
import io.github.springboot.httpclient.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.constants.ConfigurationConstants;
import io.github.springboot.httpclient.internal.RequestConfigurer;
import io.github.springboot.httpclient.utils.HttpClientUtils;
import io.github.resilience4j.retry.RetryConfig;

/**
 * Wrapper de CloseableHttpClient.
 */
@SuppressWarnings("deprecation")
@Component
public class ConfigurableHttpClient extends CloseableHttpClient {

  private static final String DEFAULT_CIRCUIT = "default";
  private final CloseableHttpClient client;
  private final RequestConfigurer configurer;

  private final CircuitBreakerRegistry cbRegistry;
  private final RateLimiterRegistry rlregstry;
  private final HttpClientConfigurationHelper config;

  // TODO Chain of responsability of client.doExecute() inspired from javax.servlet.Filter 
  public ConfigurableHttpClient(HttpClientConfigurationHelper config, CircuitBreakerRegistry cbRegistry,
      RateLimiterRegistry rlregstry, final CloseableHttpClient client, final RequestConfigurer configurer) {
    this.config = config;
    this.cbRegistry = cbRegistry;
    this.rlregstry = rlregstry;
    this.client = client;
    this.configurer = configurer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected CloseableHttpResponse doExecute(final HttpHost target, final HttpRequest request, final HttpContext context)
      throws IOException, ClientProtocolException {
    final String requestUri = HttpClientUtils.getUri(request, context).toString();
    final String circuitName = config.getConfigurationKeyForRequestUri(requestUri, DEFAULT_CIRCUIT);

    final CircuitBreaker circuitBreaker = cbRegistry.circuitBreaker(circuitName);

    if (circuitBreaker.tryAcquirePermission()) {
      final long start = System.nanoTime();
      configurer.configureRequest(target, request, context);

      final Retry retry = Retry.of(circuitName, getRetryConfig(requestUri, request.getRequestLine().getMethod()));
      final Context<CloseableHttpResponse> retryContext = retry.context();

      while (true) {
        try {
          if (circuitName != DEFAULT_CIRCUIT) {
            final RateLimiter rateLimiter = rlregstry.rateLimiter(circuitName);
            RateLimiter.waitForPermission(rateLimiter);
          }

          final CloseableHttpResponse response = client.doExecute(target, request, context);
          final StatusLine statusLine = response.getStatusLine();
          final long durationInNanos = System.nanoTime() - start;
          if (isError(statusLine)) {
            circuitBreaker.onError(durationInNanos, TimeUnit.NANOSECONDS, new Error("Http Status Error " + statusLine));
          } else {
            circuitBreaker.onSuccess(durationInNanos, TimeUnit.NANOSECONDS);
          }
          final boolean validationOfResult = retryContext.onResult(response);
          if (!validationOfResult) {
            retryContext.onComplete();
            return response;
          }
        } catch (final Throwable throwable) {
          final long durationInNanos = System.nanoTime() - start;
          circuitBreaker.onError(durationInNanos, TimeUnit.NANOSECONDS, throwable);
          retryContext.onRuntimeError(new RuntimeException(throwable));
        }
      }
    } else {
      return brokenCircuitResponse(requestUri, circuitBreaker);
    }

  }

  private RetryConfig getRetryConfig(String requestUri, String method) {
    final Integer maxAttempts = config.getConfiguration(requestUri, method, ConfigurationConstants.RETRY_ATTEMPTS);
    final Integer waitDuration = config.getConfiguration(requestUri, method,
        ConfigurationConstants.RETRY_WAIT_DURATION);
    final RetryConfig retryConfig = RetryConfig.custom().maxAttempts(maxAttempts == null ? 1 : maxAttempts)
        .waitDuration(Duration.ofMillis(waitDuration == null ? 10 : waitDuration)).build();
    return retryConfig;
  }

  private boolean isError(StatusLine statusLine) {
    return statusLine.getStatusCode() >= 500;
  }

  private CloseableHttpResponse brokenCircuitResponse(String uri, CircuitBreaker circuitBreaker)
      throws ClientProtocolException {
    final String action = config.getConfiguration(uri, ConfigurationConstants.ON_BROKEN_CIRCUIT);

    if (StringUtils.isNumeric(action)) {
      return new BasicClosableHttpResponse(HttpVersion.HTTP_1_1, Integer.parseInt(action),
          "Broken circuit : " + circuitBreaker.toString());
    } else {
      throw new ClientProtocolException(circuitBreaker.toString() + " is closed");
    }
  }

  public static class BasicClosableHttpResponse extends BasicHttpResponse implements CloseableHttpResponse {

    public BasicClosableHttpResponse(ProtocolVersion ver, int code, String reason) {
      super(ver, code, reason);
    }

    @Override
    public void close() throws IOException {
    }
  }

  @Override
  public HttpParams getParams() {
    return client.getParams();
  }

  @Override
  public ClientConnectionManager getConnectionManager() {
    return client.getConnectionManager();
  }

  @Override
  public void close() throws IOException {
    client.close();
  }

}
