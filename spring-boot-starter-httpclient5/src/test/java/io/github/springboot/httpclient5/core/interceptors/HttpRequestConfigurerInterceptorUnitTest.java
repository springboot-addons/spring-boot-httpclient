package io.github.springboot.httpclient5.core.interceptors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;
import io.github.springboot.httpclient5.core.config.model.SimplePredefinedCredentialsProvider;

class HttpRequestConfigurerInterceptorUnitTest {

	private RequestConfigProperties properties;
	private HttpRequestConfigurerInterceptor interceptor;
	private HttpClientContext context;

	@BeforeEach
	void setUp() {
		properties = new RequestConfigProperties();
		properties.setResponseTimeout(Timeout.ofSeconds(4));

		HttpClient5Config config = new HttpClient5Config() {
			@Override
			public RequestConfigProperties getRequestConfigProperties(String method, String uri) {
				return properties;
			}
		};
		interceptor = new HttpRequestConfigurerInterceptor(config);
		context = HttpClientContext.create();
	}

	@Test
	void shouldApplyRequestConfiguration() throws Exception {
		BasicHttpRequest request = new BasicHttpRequest("GET", "https://example.com/a");

		interceptor.process(request, null, context);

		assertNotNull(context.getRequestConfig());
		assertEquals(4000, context.getRequestConfig().getResponseTimeout().toMilliseconds());
	}

	@Test
	void shouldAddPreemptiveAuthorizationHeader() throws Exception {
		properties.setCredentials(new SimplePredefinedCredentialsProvider("PREEMPTIVE(admin:pwd)"));
		BasicHttpRequest request = new BasicHttpRequest("GET", "https://example.com/a");

		interceptor.process(request, null, context);

		String expected = "Basic "
				+ Base64.getEncoder().encodeToString("admin:pwd".getBytes(StandardCharsets.UTF_8));
		assertEquals(expected, java.util.Optional.ofNullable(request.getFirstHeader(HttpHeaders.AUTHORIZATION))
				.map(header -> header.getValue())
				.orElse(null));
	}

	@Test
	void shouldKeepExistingAuthorizationHeader() throws Exception {
		properties.setCredentials(new SimplePredefinedCredentialsProvider("PREEMPTIVE(admin:pwd)"));
		BasicHttpRequest request = new BasicHttpRequest("GET", "https://example.com/a");
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");

		interceptor.process(request, null, context);

		assertEquals("Bearer token", request.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue());
	}

	@Test
	void shouldRegisterNonPreemptiveCredentialsProvider() throws Exception {
		SimplePredefinedCredentialsProvider credentials = new SimplePredefinedCredentialsProvider("admin:pwd");
		properties.setCredentials(credentials);
		BasicHttpRequest request = new BasicHttpRequest("GET", "https://example.com/a");

		interceptor.process(request, null, context);

		CredentialsProvider provider = context.getCredentialsProvider();
		assertEquals(credentials, provider);
		assertFalse(request.containsHeader(HttpHeaders.AUTHORIZATION));
	}

	@Test
	void shouldPublishCustomRequestContextAttributes() throws Exception {
		properties.setCustomRequestContext(Map.of("propA", "value"));
		BasicHttpRequest request = new BasicHttpRequest("GET", "https://example.com/a");

		interceptor.process(request, null, context);

		assertEquals("value", context.getAttribute("propA"));
		assertTrue(context.getAttribute(HttpRequestConfigurerInterceptor.REQUEST_CONFIG_EXTENDED) != null);
	}

	@Test
	void shouldCleanupCustomAttributesAfterResponse() throws Exception {
		properties.setCustomRequestContext(Map.of("propA", "value"));
		BasicHttpRequest request = new BasicHttpRequest("GET", "https://example.com/a");
		interceptor.process(request, null, context);

		new HttpRequestConfigurerInterceptorPostCleanup().process(new BasicHttpResponse(200), null, context);

		assertNull(context.getAttribute("propA"));
		assertNull(context.getAttribute(HttpRequestConfigurerInterceptor.REQUEST_CONFIG_EXTENDED));
	}
}
