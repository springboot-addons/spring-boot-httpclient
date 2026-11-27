package io.github.springboot.httpclient5.core.config.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Test;

class RequestConfigPropertiesTest {

	@Test
	void shouldCopyConfigurationWithoutSharingMutableCollections() {
		RequestConfigProperties origin = new RequestConfigProperties();
		origin.setResponseTimeout(Timeout.ofSeconds(5));
		origin.getInterceptors().put("interceptor", true);
		origin.getCustomRequestContext().put("key", "value");

		RequestConfigProperties copy = new RequestConfigProperties(origin);
		copy.getInterceptors().put("other", true);
		copy.getCustomRequestContext().put("other", "value");

		assertEquals(5000, copy.build().getResponseTimeout().toMilliseconds());
		assertTrue(origin.getInterceptors().containsKey("interceptor"));
		assertTrue(!origin.getInterceptors().containsKey("other"));
		assertTrue(!origin.getCustomRequestContext().containsKey("other"));
	}

	@Test
	void shouldCopyNestedConfiguration() {
		RequestConfigProperties origin = new RequestConfigProperties();
		origin.setRetryConfig(RetryConfig.builder().maxRetries(3).retryInterval(TimeValue.ofSeconds(2)).build());
		origin.setCredentials(new SimplePredefinedCredentialsProvider("PREEMPTIVE(admin:pwd)"));

		RequestConfigProperties copy = new RequestConfigProperties(origin);

		assertEquals(3, copy.getRetryConfig().getMaxRetries());
		assertEquals(2000, copy.getRetryConfig().getRetryInterval().toMilliseconds());
		assertTrue(copy.getCredentials().isPreemptive());
		assertNotSame(origin.getCredentials(), copy.getCredentials());
	}

	@Test
	void shouldApplyNonDefaultValuesAndMergeMaps() {
		RequestConfigProperties base = new RequestConfigProperties();
		base.getCustomRequestContext().put("base", "value");
		base.getInterceptors().put("base", false);

		RequestConfigProperties override = new RequestConfigProperties();
		override.setResponseTimeout(Timeout.ofSeconds(3));
		override.getCustomRequestContext().put("override", "value");
		override.getInterceptors().put("override", true);

		RequestConfigProperties result = base.apply(override);

		assertEquals(3000, result.build().getResponseTimeout().toMilliseconds());
		assertEquals(Map.of("base", "value", "override", "value"), result.getCustomRequestContext());
		assertEquals(Map.of("base", false, "override", true), result.getInterceptors());
	}

	@Test
	void shouldNotOverrideValuesWithDefaults() {
		RequestConfigProperties base = new RequestConfigProperties();
		base.setResponseTimeout(Timeout.ofSeconds(3));

		RequestConfigProperties result = base.apply(new RequestConfigProperties());

		assertEquals(3000, result.build().getResponseTimeout().toMilliseconds());
	}

	@Test
	void shouldStartFromHttpClientDefaultsWhenOriginIsNull() {
		RequestConfigProperties properties = new RequestConfigProperties(null);

		assertEquals(RequestConfig.DEFAULT.getConnectionRequestTimeout(),
				properties.build().getConnectionRequestTimeout());
	}

	@Test
	void shouldCopyAndApplyAdaptiveTimeoutRule() {
		RequestConfigProperties origin = new RequestConfigProperties();
		origin.setAdaptiveTimeout(AdaptiveTimeoutRuleProperties.builder().enabled(false).build());

		RequestConfigProperties copy = new RequestConfigProperties(origin);
		assertNotSame(origin.getAdaptiveTimeout(), copy.getAdaptiveTimeout());
		assertTrue(!copy.getAdaptiveTimeout().getEnabled());

		RequestConfigProperties base = new RequestConfigProperties();
		RequestConfigProperties override = new RequestConfigProperties();
		override.setAdaptiveTimeout(AdaptiveTimeoutRuleProperties.builder().enabled(true).build());

		RequestConfigProperties result = base.apply(override);

		assertTrue(result.getAdaptiveTimeout().getEnabled());
	}

	@Test
	void shouldProvideEmptyHeaderPropagationCollectionsFromTheBuilder() {
		HeadersPropagationProperties properties = HeadersPropagationProperties.builder().build();

		assertTrue(properties.getDown() != null && properties.getDown().isEmpty());
		assertTrue(properties.getUp() != null && properties.getUp().isEmpty());
		assertTrue(properties.getAdd() != null && properties.getAdd().isEmpty());
		assertTrue(properties.getRemove() != null && properties.getRemove().isEmpty());
	}
}
