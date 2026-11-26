package io.github.springboot.httpclient.web.headers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.HeadersPropagationProperties;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;

class ForwardedRequestHeadersCollectorTest {

	private final RequestHeadersProviders.DefaultRequestHeadersStorage downHeaders = new RequestHeadersProviders.DefaultRequestHeadersStorage();
	private ForwardedRequestHeadersCollector collector;

	@BeforeEach
	void setUp() {
		HeadersPropagationProperties properties = new HeadersPropagationProperties();
		properties.setDown(List.of("X-TEST-.*"));

		RequestConfigProperties requestConfigProperties = new RequestConfigProperties();
		requestConfigProperties.setHeadersPropagation(properties);

		HttpClient5Config config = new HttpClient5Config() {
			@Override
			public RequestConfigProperties getRequestConfigProperties(String method, String uri) {
				return requestConfigProperties;
			}
		};

		collector = new ForwardedRequestHeadersCollector();
		ReflectionTestUtils.setField(collector, "config", config);
		ReflectionTestUtils.setField(collector, "downHeaders", provider(downHeaders));
	}

	@Test
	void shouldSupportConfiguredHeaderPatterns() {
		assertTrue(collector.supports("GET", "https://example.com/a", "X-TEST-KEY"));
		assertFalse(collector.supports("GET", "https://example.com/a", "Authorization"));
	}

	@Test
	void shouldStoreHeaderValues() {
		collector.handle("X-TEST-KEY", "value");

		assertEquals(1, downHeaders.getHeaderList().size());
		assertEquals("X-TEST-KEY", downHeaders.getHeaderList().get(0).getName());
		assertEquals("value", downHeaders.getHeaderList().get(0).getValue());
	}

	@Test
	void shouldStoreAllHeaderValues() {
		collector.handle("X-TEST-KEY", Collections.enumeration(List.of("value1", "value2")));

		assertEquals(2, downHeaders.getHeaderList().size());
	}

	private ObjectProvider<RequestHeadersProviders.RequestHeadersStorage> provider(
			RequestHeadersProviders.RequestHeadersStorage storage) {
		StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(Map.of("storage", storage));
		return beanFactory.getBeanProvider(RequestHeadersProviders.RequestHeadersStorage.class);
	}
}
