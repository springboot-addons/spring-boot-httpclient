package io.github.springboot.httpclient.web.headers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.ServletResponseWrapper;
import jakarta.servlet.http.HttpServletResponse;

class HeadersFilterTest {

	@Test
	void shouldCollectRequestHeadersAndAddResponseHeaders() throws Exception {
		RequestHeadersProviders.DefaultRequestHeadersStorage storage = new RequestHeadersProviders.DefaultRequestHeadersStorage();
		storage.add("stale", "value");

		RequestHeaderCollector collector = new RequestHeaderCollector() {
			@Override
			public boolean supports(String method, String uri, String headerName) {
				return "X-TEST-KEY".equalsIgnoreCase(headerName);
			}

			@Override
			public void handle(String headerName, String headerValue) {
				storage.add(headerName, headerValue);
			}

			@Override
			public void handle(String headerName, Enumeration<String> headerValues) {
				storage.add(headerName, headerValues);
			}
		};

		ResponseHeaderProvider provider = new ResponseHeaderProvider() {
			@Override
			public List<String> getHeaderNames(String method, String uri) {
				return List.of("X-RESPONSE");
			}

			@Override
			public List<String> getHeaderValues(String headerName) {
				return List.of("ok");
			}
		};

		HeadersFilter filter = new HeadersFilter();
		ReflectionTestUtils.setField(filter, "requestHeaderCollectors", List.of(collector));
		ReflectionTestUtils.setField(filter, "responseHeaderProviders", List.of(provider));
		ReflectionTestUtils.setField(filter, "headersStorages", storageProvider(storage));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "http://example.com/front/header");
		request.addHeader("X-TEST-KEY", "srules");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain chain = (req, res) -> {
			((HttpServletResponse) res).addHeader("X-FROM-CHAIN", "yes");
			res.getWriter().write("body");
		};

		filter.doFilter(request, response, chain);

		assertEquals("ok", response.getHeader("X-RESPONSE"));
		assertEquals("yes", response.getHeader("X-FROM-CHAIN"));
		assertEquals("body", response.getContentAsString());
		assertTrue(storage.getHeaderList().stream().anyMatch(h -> "X-TEST-KEY".equals(h.getName())));
		assertTrue(storage.getHeaderList().stream().noneMatch(h -> "stale".equals(h.getName())));
	}

	@Test
	void shouldPassThroughNonHttpRequests() throws Exception {
		RequestHeadersProviders.DefaultRequestHeadersStorage storage = new RequestHeadersProviders.DefaultRequestHeadersStorage();
		HeadersFilter filter = new HeadersFilter();
		ReflectionTestUtils.setField(filter, "requestHeaderCollectors", List.of());
		ReflectionTestUtils.setField(filter, "responseHeaderProviders", List.of());
		ReflectionTestUtils.setField(filter, "headersStorages", storageProvider(storage));

		jakarta.servlet.ServletRequest request = new ServletRequestWrapper(new MockHttpServletRequest());
		jakarta.servlet.ServletResponse response = new ServletResponseWrapper(new MockHttpServletResponse());
		final boolean[] called = { false };

		FilterChain chain = (req, res) -> called[0] = true;

		filter.doFilter(request, response, chain);

		assertTrue(called[0]);
	}

	@Test
	void shouldExposeHeaderStorage() {
		RequestHeadersProviders.DefaultRequestHeadersStorage storage = new RequestHeadersProviders.DefaultRequestHeadersStorage();
		storage.add("X-ONE", "1");
		storage.add("X-ONE", "2");

		assertEquals(2, storage.getHeaderList().size());
		storage.clear();
		assertEquals(0, storage.getHeaderList().size());
	}

	private ObjectProvider<RequestHeadersProviders.RequestHeadersStorage> storageProvider(
			RequestHeadersProviders.RequestHeadersStorage storage) {
		StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(Map.of("storage", storage));
		ObjectProvider<RequestHeadersProviders.RequestHeadersStorage> provider = beanFactory
				.getBeanProvider(RequestHeadersProviders.RequestHeadersStorage.class);
		assertNotNull(provider);
		return provider;
	}
}
