package io.github.springboot.httpclient5.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.springboot.httpclient5.core.config.model.ConnectionConfigProperties;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;

class HttpClient5ConfigTest {

	@Test
	void shouldApplyZeroConfigurationDefaults() {
		HttpClient5Config config = config(new MockEnvironment());

		config.init();

		assertTrue(config.getRequestConfig().containsKey(HttpClient5Config.DEFAULT_HOST_KEY));
		assertEquals(HttpClient5Config.DEFAULT_RESPONSE_TIMEOUT,
				config.getDefaultRequestConfig().build().getResponseTimeout());
		assertNotNull(config.getPool().getDefaultConnectionConfig());
		assertNotNull(config.getPool().getDefaultSocketConfig());

		PoolingHttpClientConnectionManager connectionManager = config.getPool().get().build();
		assertEquals(50, connectionManager.getDefaultMaxPerRoute());
	}

	@Test
	void shouldApplyConnectionDefaultsEvenWhenMaxConnPerRouteIsConfigured() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.httpclient5.pool.max-connper-route", "10");
		HttpClient5Config config = config(environment);

		config.init();

		assertNotNull(config.getPool().getDefaultConnectionConfig());
		assertNotNull(config.getPool().getDefaultSocketConfig());
		assertFalse(config.getRequestConfig().isEmpty());
	}

	@Test
	void shouldExpandPlaceholdersInPatternKeys() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("app.host", "https://example.com");
		HttpClient5Config config = config(environment);
		config.setAutoconfig(false);
		config.getRequestConfig().put("GET ${app.host}/.*", new RequestConfigProperties());

		config.init();

		assertTrue(config.getRequestConfig().containsKey("GET https://example.com/.*"));
	}

	@Test
	void shouldNormalizeHostConfigurationKeys() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("app.host", "https://example.com");
		HttpClient5Config config = config(environment);
		config.setAutoconfig(false);
		config.getPool().getHostConfig().put("${app.host}", new ConnectionConfigProperties());
		config.getPool().getHostConfig().put("http://plain.example.com", new ConnectionConfigProperties());

		config.init();

		assertTrue(config.getPool().getHostConfig().containsKey("https://example.com:443"));
		assertTrue(config.getPool().getHostConfig().containsKey("http://plain.example.com:80"));
	}

	@Test
	void shouldKeepExplicitPortInHostConfigurationKeys() {
		HttpClient5Config config = config(new MockEnvironment());
		config.setAutoconfig(false);
		config.getPool().getHostConfig().put("https://example.com:8443", new ConnectionConfigProperties());

		config.init();

		assertTrue(config.getPool().getHostConfig().containsKey("https://example.com:8443"));
	}

	@Test
	void shouldNotAddDefaultRequestConfigurationWhenAutoconfigIsDisabled() {
		HttpClient5Config config = config(new MockEnvironment());
		config.setAutoconfig(false);

		config.init();

		assertTrue(config.getRequestConfig().isEmpty());
	}

	@Test
	void shouldExposeAdaptiveTimeoutDefaults() {
		HttpClient5Config config = config(new MockEnvironment());

		assertFalse(config.getAdaptiveTimeout().isEnabled());
		assertEquals(0.99d, config.getAdaptiveTimeout().getPercentile());
		assertEquals(1.5d, config.getAdaptiveTimeout().getFactor());
		assertEquals(2.0d, config.getAdaptiveTimeout().getLowerBoundFactor());
		assertEquals(60_000, config.getAdaptiveTimeout().getMaxTimeout().toMilliseconds());
	}

	@Test
	void shouldApplyAdaptiveTimeoutPolicyToResolvedRequestConfig() {
		HttpClient5Config config = config(new MockEnvironment());
		config.setAutoconfig(false);
		RequestConfigProperties rule = new RequestConfigProperties();
		rule.setResponseTimeout(Timeout.ofSeconds(30));
		config.getRequestConfig().put(HttpClient5Config.DEFAULT_HOST_KEY, rule);

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("adaptiveTimeoutPolicy", (AdaptiveTimeoutPolicy) (method, uri, properties) -> Timeout.ofSeconds(7));
		ReflectionTestUtils.setField(config, "adaptiveTimeoutPolicyProvider",
				beanFactory.getBeanProvider(AdaptiveTimeoutPolicy.class));

		assertEquals(7000, config.getRequestConfig("GET", "http://example.com/").getResponseTimeout().toMilliseconds());
	}

	private HttpClient5Config config(MockEnvironment environment) {
		HttpClient5Config config = new HttpClient5Config();
		config.setEnv(environment);
		return config;
	}
}
