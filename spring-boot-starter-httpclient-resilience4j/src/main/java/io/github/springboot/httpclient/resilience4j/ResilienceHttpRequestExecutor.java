package io.github.springboot.httpclient.resilience4j;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpContext;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.Retry.Context;
import io.github.resilience4j.retry.RetryConfig;
import io.github.springboot.httpclient.core.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.core.constants.ConfigurationConstants;
import io.github.springboot.httpclient.core.internal.ChainableHttpRequestExecutor;
import io.github.springboot.httpclient.core.internal.HttpRequestExecutorChain;
import io.github.springboot.httpclient.core.utils.HttpClientUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * ResilienceHttpRequestExecutor
 */
@Slf4j
public class ResilienceHttpRequestExecutor implements ChainableHttpRequestExecutor {

	private final CircuitBreakerRegistry cbRegistry;
	private final RateLimiterRegistry rlregstry;
	private final HttpClientConfigurationHelper config;

	// TODO Chain of responsability of client.doExecute() inspired from
	// javax.servlet.Filter
	public ResilienceHttpRequestExecutor(HttpClientConfigurationHelper config, CircuitBreakerRegistry cbRegistry,
			RateLimiterRegistry rlregstry) {
		this.config = config;
		this.cbRegistry = cbRegistry;
		this.rlregstry = rlregstry;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HttpResponse doExecute(HttpRequest request, HttpClientConnection conn, HttpContext context,
			HttpRequestExecutorChain chain) throws IOException, HttpException {
		final String requestUri = HttpClientUtils.getUri(request, context).toString();
		final String circuitName = config.getConfigurationKeyForRequestUri(requestUri,
				HttpClientResilience4jAutoConfiguration.DEFAULT_CIRCUIT);

		final CircuitBreaker circuitBreaker = cbRegistry.circuitBreaker(circuitName);
		log.debug("Before circuit breakers {} state {}, metrics {}", circuitBreaker.getName(), circuitBreaker.getState(), ToStringBuilder.reflectionToString(circuitBreaker.getMetrics())) ;

		if (circuitBreaker.tryAcquirePermission()) {
			final long start = System.nanoTime();
			final Retry retry = Retry.of(circuitName, getRetryConfig(requestUri, request.getRequestLine().getMethod()));
			final Context<HttpResponse> retryContext = retry.context();

			while (true) {
				try {
					if (circuitName != HttpClientResilience4jAutoConfiguration.DEFAULT_CIRCUIT) {
						final RateLimiter rateLimiter = rlregstry.rateLimiter(circuitName);
						RateLimiter.waitForPermission(rateLimiter);
					}

					final HttpResponse response = chain.doExecute(request, conn, context);
					final StatusLine statusLine = response.getStatusLine();
					final long durationInNanos = System.nanoTime() - start;
					if (isError(statusLine)) {
						circuitBreaker.onError(durationInNanos, TimeUnit.NANOSECONDS,
								new IOException("Http Status Error " + statusLine));
						log.debug("After http 5xx circuit breakers state {}, metrics {}", circuitBreaker.getState(), ToStringBuilder.reflectionToString(circuitBreaker.getMetrics())) ;
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
					log.debug("Aftern exception circuit breakers state {}, metrics {}", circuitBreaker.getState(), ToStringBuilder.reflectionToString(circuitBreaker.getMetrics())) ;
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
}
