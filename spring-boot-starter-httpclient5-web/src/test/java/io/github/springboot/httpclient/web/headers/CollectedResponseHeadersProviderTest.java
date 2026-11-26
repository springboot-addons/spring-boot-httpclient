package io.github.springboot.httpclient.web.headers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class CollectedResponseHeadersProviderTest {

	private final RequestHeadersProviders.DefaultRequestHeadersStorage upHeaders = new RequestHeadersProviders.DefaultRequestHeadersStorage();
	private CollectedResponseHeadersProvider provider;

	@BeforeEach
	void setUp() {
		HeadersPropagationProperties properties = new HeadersPropagationProperties();
		properties.setUp(List.of("X-UP-.*"));

		RequestConfigProperties requestConfigProperties = new RequestConfigProperties();
		requestConfigProperties.setHeadersPropagation(properties);

		HttpClient5Config config = new HttpClient5Config() {
			@Override
			public RequestConfigProperties getRequestConfigProperties(String method, String uri) {
				return requestConfigProperties;
			}
		};

		provider = new CollectedResponseHeadersProvider();
		ReflectionTestUtils.setField(provider, "config", config);
		StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(Map.of("storage", upHeaders));
		ObjectProvider<RequestHeadersProviders.RequestHeadersStorage> storageProvider = beanFactory
				.getBeanProvider(RequestHeadersProviders.RequestHeadersStorage.class);
		ReflectionTestUtils.setField(provider, "upHeadersProvider", storageProvider);
	}

	@Test
	void shouldOnlyExposeConfiguredHeaderNames() {
		upHeaders.add("X-UP-1", "value1");
		upHeaders.add("X-OTHER", "ignored");

		List<String> names = provider.getHeaderNames("GET", "https://example.com/a");

		assertEquals(List.of("X-UP-1"), names);
	}

	@Test
	void shouldExposeAllValuesOfAHeaderCaseInsensitively() {
		upHeaders.add("X-UP-1", "value1");
		upHeaders.add("X-UP-1", "value2");

		List<String> values = provider.getHeaderValues("x-up-1");

		assertTrue(values.containsAll(List.of("value1", "value2")));
	}
}
