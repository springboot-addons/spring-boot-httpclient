package io.github.springboot.httpclient.web.headers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.HeadersPropagationProperties;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;

class HeadersPropagationInterceptorTest {

	private final RequestHeadersProviders.DefaultRequestHeadersStorage downHeaders = new RequestHeadersProviders.DefaultRequestHeadersStorage();
	private final RequestHeadersProviders.DefaultRequestHeadersStorage upHeaders = new RequestHeadersProviders.DefaultRequestHeadersStorage();
	private HeadersPropagationProperties properties;
	private HeadersPropagationInterceptor interceptor;
	private HttpClientContext context;
	private BasicHttpRequest request;

	@BeforeEach
	void setUp() {
		properties = new HeadersPropagationProperties();
		properties.setEnabled(true);
		properties.setDown(List.of("X-TEST-.*"));
		properties.setUp(List.of("X-UP-.*"));
		properties.setAdd(Map.of("X-ADDED", "added-value"));
		properties.setRemove(List.of("X-REMOVED"));

		RequestConfigProperties requestConfigProperties = new RequestConfigProperties();
		requestConfigProperties.setHeadersPropagation(properties);

		HttpClient5Config config = new HttpClient5Config() {
			@Override
			public RequestConfigProperties getRequestConfigProperties(String method, String uri) {
				return requestConfigProperties;
			}
		};

		interceptor = new HeadersPropagationInterceptor();
		ReflectionTestUtils.setField(interceptor, "config", config);
		ReflectionTestUtils.setField(interceptor, "downHeadersProvider", provider(downHeaders));
		ReflectionTestUtils.setField(interceptor, "upHeadersProvider", provider(upHeaders));

		request = new BasicHttpRequest("GET", "https://example.com/a");
		request.addHeader("X-REMOVED", "value");
		context = HttpClientContext.create();
		context.setRequest(request);
	}

	@Test
	void shouldPropagateMatchingHeadersToTheRequest() throws Exception {
		downHeaders.add("X-TEST-KEY", "srules");
		downHeaders.add("X-OTHER", "ignored");

		interceptor.process(request, null, context);

		assertEquals("srules", request.getFirstHeader("X-TEST-KEY").getValue());
		assertNull(request.getFirstHeader("X-OTHER"));
		assertEquals("added-value", request.getFirstHeader("X-ADDED").getValue());
		assertNull(request.getFirstHeader("X-REMOVED"));
	}

	@Test
	void shouldNotPropagateWhenDisabled() throws Exception {
		properties.setEnabled(false);
		downHeaders.add("X-TEST-KEY", "srules");

		interceptor.process(request, null, context);

		assertNull(request.getFirstHeader("X-TEST-KEY"));
		assertNull(request.getFirstHeader("X-ADDED"));
		assertTrue(request.containsHeader("X-REMOVED"));
	}

	@Test
	void shouldStoreMatchingResponseHeaders() throws Exception {
		BasicHttpResponse response = new BasicHttpResponse(200);
		response.addHeader("X-UP-1", "value1");
		response.addHeader("X-OTHER", "ignored");

		interceptor.process(response, null, context);

		assertFalse(upHeaders.getHeaderList().isEmpty());
		assertTrue(upHeaders.getHeaderList().stream().anyMatch(h -> "X-UP-1".equals(h.getName())));
		assertFalse(upHeaders.getHeaderList().stream().anyMatch(h -> "X-OTHER".equals(h.getName())));
	}

	@Test
	void shouldIgnoreDisabledRequestConfiguration() {
		HttpClient5Config config = new HttpClient5Config() {
			@Override
			public RequestConfigProperties getRequestConfigProperties(String method, String uri) {
				return new RequestConfigProperties();
			}
		};
		HeadersPropagationInterceptor interceptor = new HeadersPropagationInterceptor();
		ReflectionTestUtils.setField(interceptor, "config", config);
		ReflectionTestUtils.setField(interceptor, "downHeadersProvider", provider(downHeaders));
		ReflectionTestUtils.setField(interceptor, "upHeadersProvider", provider(upHeaders));

		assertDoesNotThrow(() -> interceptor.process(request, null, context));
	}

	private ObjectProvider<RequestHeadersProviders.RequestHeadersStorage> provider(
			RequestHeadersProviders.RequestHeadersStorage storage) {
		StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(Map.of("storage", storage));
		return beanFactory.getBeanProvider(RequestHeadersProviders.RequestHeadersStorage.class);
	}
}
