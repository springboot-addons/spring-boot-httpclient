package io.github.springboot.httpclient5.core.configure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Test;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;

class CustomHttpRoutePlannerTest {

	@Test
	void shouldApplyRequestConfigurationWhileComputingTheRoute() throws Exception {
		RequestConfigProperties properties = new RequestConfigProperties();
		properties.setResponseTimeout(Timeout.ofSeconds(7));

		HttpClient5Config config = new HttpClient5Config() {
			@Override
			public RequestConfigProperties getRequestConfigProperties(String method, String uri) {
				return properties;
			}
		};

		CustomHttpRoutePlanner planner = new CustomHttpRoutePlanner(config);
		HttpClientContext context = HttpClientContext.create();
		BasicHttpRequest request = new BasicHttpRequest("GET", "https://example.com/a");
		context.setRequest(request);

		HttpRoute route = planner.determineRoute(new HttpHost("https", "example.com", 443), request, context);

		assertEquals("example.com", route.getTargetHost().getHostName());
		assertEquals(7000, context.getRequestConfig().getResponseTimeout().toMilliseconds());
	}
}
