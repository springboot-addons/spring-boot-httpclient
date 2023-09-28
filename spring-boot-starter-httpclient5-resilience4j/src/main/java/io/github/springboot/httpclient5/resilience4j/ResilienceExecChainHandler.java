package io.github.springboot.httpclient5.resilience4j;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.ExecChain;
import org.apache.hc.client5.http.classic.ExecChain.Scope;
import org.apache.hc.client5.http.classic.ExecChainHandler;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.Retry.Context;
import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;
import io.github.resilience4j.retry.RetryConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * ResilienceExecChainHandler
 */
@Slf4j
public class ResilienceExecChainHandler implements ExecChainHandler {

	private final CircuitBreakerRegistry cbRegistry;
	private final RateLimiterRegistry rlregstry;
	private final HttpClient5Config config;

	// TODO Chain of responsability of client.doExecute() inspired from
	// javax.servlet.Filter
	public ResilienceExecChainHandler(HttpClient5Config config, CircuitBreakerRegistry cbRegistry,
			RateLimiterRegistry rlregstry) {
		this.config = config;
		this.cbRegistry = cbRegistry;
		this.rlregstry = rlregstry;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SneakyThrows
	public ClassicHttpResponse execute(ClassicHttpRequest request, Scope scope, ExecChain chain)
			throws IOException, HttpException {
		String method = request.getMethod();
		String requestUri = request.getUri().toString();
		RequestConfigProperties requestConfigProperties = config.getRequestConfigProperties(method, requestUri);
		String circuitName = requestConfigProperties.getErrorManagement().getCircuitName() ;

		final CircuitBreaker circuitBreaker = cbRegistry.circuitBreaker(circuitName);
		if (log.isTraceEnabled()) {
			log.trace("Before circuit breakers {} state {}, metrics {}", circuitBreaker.getName(), circuitBreaker.getState(), ToStringBuilder.reflectionToString(circuitBreaker.getMetrics())) ;
		}

		if (circuitBreaker.tryAcquirePermission()) {
			final long start = System.nanoTime();
			final Retry retry = Retry.of(circuitName, getRetryConfig(requestConfigProperties));
			final Context<HttpResponse> retryContext = retry.context();

			while (true) {
				try {
					if (circuitName != HttpClientResilience4jAutoConfiguration.DEFAULT_CIRCUIT) {
						final RateLimiter rateLimiter = rlregstry.rateLimiter(circuitName);
						RateLimiter.waitForPermission(rateLimiter);
					}

					final ClassicHttpResponse response = chain.proceed(request, scope);
					final int statusCode = response.getCode();
					final long durationInNanos = System.nanoTime() - start;
					if (isError(statusCode)) {
						circuitBreaker.onError(durationInNanos, TimeUnit.NANOSECONDS,
								new IOException("Http Status Error " + statusCode));
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
					log.debug("After exception circuit breakers state {}, metrics {}", circuitBreaker.getState(), ToStringBuilder.reflectionToString(circuitBreaker.getMetrics())) ;
					retryContext.onRuntimeError(new RuntimeException(throwable));
				}
			}
		} else {
			return brokenCircuitResponse(circuitBreaker, requestConfigProperties.getErrorManagement().getBrokenCircuitAction());
		}

	}

	private RetryConfig getRetryConfig(RequestConfigProperties requestConfigProperties) {
		final Integer maxAttempts = requestConfigProperties.getErrorManagement().getMaxAttempts();
		final Integer waitDuration = requestConfigProperties.getErrorManagement().getWaitDuration();
		final RetryConfig retryConfig = RetryConfig.custom().maxAttempts(maxAttempts == null ? 1 : maxAttempts)
				.waitDuration(Duration.ofMillis(waitDuration == null ? 10 : waitDuration)).build();
		return retryConfig;
	}

	private boolean isError(int code ) {
		return code >= 500;
	}

	private BasicClassicHttpResponse brokenCircuitResponse(CircuitBreaker circuitBreaker, String action)
			throws ClientProtocolException {

		if (StringUtils.isNumeric(action)) {
			return new BasicClassicHttpResponse(Integer.parseInt(action),
					"Broken circuit : " + circuitBreaker.toString());
		} else {
			throw new ClientProtocolException("Broken circuit : " + circuitBreaker.toString() + " is closed");
		}
	}
}
