package io.github.springboot.httpclient5.core.configure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ConnectException;

import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.apache.hc.core5.util.TimeValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;
import io.github.springboot.httpclient5.core.config.model.RetryConfig;

class ConfigurableHttpRequestRetryStrategyTest {

	private HttpClient5Config config;
	private ConfigurableHttpRequestRetryStrategy strategy;

	@BeforeEach
	void setUp() {
		RequestConfigProperties properties = new RequestConfigProperties();
		properties.setRetryConfig(RetryConfig.builder()
				.maxRetries(2)
				.retryInterval(TimeValue.ofSeconds(1))
				.build());

		config = new HttpClient5Config() {
			@Override
			public RequestConfigProperties getRequestConfigProperties(String method, String uri) {
				return properties;
			}
		};
		strategy = new ConfigurableHttpRequestRetryStrategy(config);
	}

	@Test
	void shouldReadMaxRetriesFromRequestConfiguration() {
		BasicHttpRequest request = new BasicHttpRequest("GET", "http://example.com/a");
		HttpClientContext context = HttpClientContext.create();

		assertEquals(2, strategy.getMaxRetries(request, context));
		assertEquals(2, context.getAttribute("MAX_RETRIES"));
		assertEquals(TimeValue.ofSeconds(1), context.getAttribute("RETRY_INTERVAL"));
	}

	@Test
	void shouldRetryIdempotentRequestWhenUnderLimit() {
		BasicHttpRequest request = new BasicHttpRequest("GET", "http://example.com/a");

		assertTrue(strategy.retryRequest(request, new IOException("network"), 1, HttpClientContext.create()));
	}

	@Test
	void shouldNotRetryWhenLimitIsReached() {
		BasicHttpRequest request = new BasicHttpRequest("GET", "http://example.com/a");

		assertFalse(strategy.retryRequest(request, new IOException("network"), 3, HttpClientContext.create()));
	}

	@Test
	void shouldNotRetryNonIdempotentRequest() {
		BasicHttpRequest request = new BasicHttpRequest("POST", "http://example.com/a");

		assertFalse(strategy.retryRequest(request, new IOException("network"), 1, HttpClientContext.create()));
	}

	@Test
	void shouldNotRetryConnectionErrors() {
		BasicHttpRequest request = new BasicHttpRequest("GET", "http://example.com/a");

		assertFalse(strategy.retryRequest(request, new ConnectException("refused"), 1, HttpClientContext.create()));
	}

	@Test
	void shouldRetryRetriableHttpStatus() {
		BasicHttpRequest request = new BasicHttpRequest("GET", "http://example.com/a");
		HttpClientContext context = HttpClientContext.create();
		context.setRequest(request);

		BasicHttpResponse response = new BasicHttpResponse(HttpStatus.SC_SERVICE_UNAVAILABLE);

		assertTrue(strategy.retryRequest(response, 1, context));
	}

	@Test
	void shouldNotRetrySuccessfulHttpStatus() {
		BasicHttpRequest request = new BasicHttpRequest("GET", "http://example.com/a");
		HttpClientContext context = HttpClientContext.create();
		context.setRequest(request);

		BasicHttpResponse response = new BasicHttpResponse(HttpStatus.SC_OK);

		assertFalse(strategy.retryRequest(response, 1, context));
	}

	@Test
	void shouldUseRetryAfterHeaderWhenPresent() {
		BasicHttpResponse response = new BasicHttpResponse(HttpStatus.SC_SERVICE_UNAVAILABLE);
		response.addHeader(HttpHeaders.RETRY_AFTER, "3");

		TimeValue interval = strategy.getRetryInterval(response, 1, HttpClientContext.create());

		assertEquals(3000, interval.toMilliseconds());
	}

	@Test
	void shouldFallBackToConfiguredRetryInterval() {
		HttpClientContext context = HttpClientContext.create();
		context.setAttribute("RETRY_INTERVAL", TimeValue.ofMilliseconds(750));
		BasicHttpResponse response = new BasicHttpResponse(HttpStatus.SC_SERVICE_UNAVAILABLE);

		TimeValue interval = strategy.getRetryInterval(response, 1, context);

		assertEquals(750, interval.toMilliseconds());
	}

	@Test
	void shouldProvideADefaultRetryInterval() {
		BasicHttpResponse response = new BasicHttpResponse(HttpStatus.SC_SERVICE_UNAVAILABLE);

		TimeValue interval = strategy.getRetryInterval(response, 1, HttpClientContext.create());

		assertEquals(ConfigurableHttpRequestRetryStrategy.DEFAULT_RETRY_INTERVAL.toMilliseconds(),
				interval.toMilliseconds());
	}
}
