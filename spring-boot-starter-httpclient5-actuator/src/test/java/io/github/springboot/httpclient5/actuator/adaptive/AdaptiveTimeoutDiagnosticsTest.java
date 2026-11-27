package io.github.springboot.httpclient5.actuator.adaptive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.AdaptiveTimeoutProperties;
import io.github.springboot.httpclient5.core.config.model.AdaptiveTimeoutRuleProperties;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;
import io.micrometer.core.instrument.MockClock;

class AdaptiveTimeoutDiagnosticsTest {

	private MockClock clock;
	private HttpClient5Config config;
	private MicrometerAdaptiveTimeoutPolicy policy;
	private AdaptiveTimeoutDiagnostics diagnostics;

	@BeforeEach
	void setUp() {
		clock = new MockClock();
		config = new HttpClient5Config();
		AdaptiveTimeoutProperties global = config.getAdaptiveTimeout();
		global.setEnabled(false);
		global.setWarmupRequests(1);
		global.setMinSamples(1);
		global.setMaxSamples(10);
		global.setWindow(TimeValue.ofMinutes(1));
		global.setRefreshInterval(TimeValue.ZERO_MILLISECONDS);
		global.setMinTimeout(Timeout.ZERO_MILLISECONDS);
		global.setMaxTimeout(Timeout.ofSeconds(60));
		global.setPercentile(0.99d);
		global.setFactor(1.5d);
		global.setLowerBoundFactor(2.0d);
		config.setAutoconfig(false);

		RequestConfigProperties defaultRule = new RequestConfigProperties();
		defaultRule.setResponseTimeout(Timeout.ofSeconds(30));
		config.getRequestConfig().put(HttpClient5Config.DEFAULT_HOST_KEY, defaultRule);

		RequestConfigProperties enabledRule = new RequestConfigProperties();
		enabledRule.setResponseTimeout(Timeout.ofSeconds(10));
		enabledRule.setAdaptiveTimeout(AdaptiveTimeoutRuleProperties.builder().enabled(true).build());
		config.getRequestConfig().put("GET https://api.example.com/.*", enabledRule);

		RequestConfigProperties disabledRule = new RequestConfigProperties();
		disabledRule.setResponseTimeout(Timeout.ofSeconds(5));
		disabledRule.setAdaptiveTimeout(AdaptiveTimeoutRuleProperties.builder().enabled(false).build());
		config.getRequestConfig().put("GET https://api.example.com/stream/.*", disabledRule);

		RequestConfigProperties inheritedRule = new RequestConfigProperties();
		inheritedRule.setAdaptiveTimeout(AdaptiveTimeoutRuleProperties.builder().enabled(true).build());
		config.getRequestConfig().put("GET https://api.example.com/inherit/.*", inheritedRule);

		policy = new MicrometerAdaptiveTimeoutPolicy(config, clock);
		policy.record(TimeValue.ofMilliseconds(10).toNanoseconds());
		policy.resolveTimeout("GET", "http://example.com/", propertiesEnabled());
		diagnostics = new AdaptiveTimeoutDiagnostics(config, policy);
	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldExposeGlobalStateAndCurrentValues() {
		Map<String, Object> snapshot = diagnostics.snapshot();

		assertEquals("READY", snapshot.get("status"));
		assertEquals(false, snapshot.get("globalEnabled"));
		assertEquals(true, snapshot.get("ready"));
		assertEquals(false, snapshot.get("stale"));

		Map<String, Object> current = (Map<String, Object>) snapshot.get("current");
		assertEquals(20L, current.get("timeoutMs"));
		assertEquals(10L, current.get("percentileMs"));
		assertEquals(10L, current.get("minObservedMs"));
		assertEquals(1, current.get("samples"));
		assertEquals(1L, current.get("totalSamples"));
		assertEquals(0.0d, current.get("timeoutRate"));
	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldCompareConfiguredAndAdaptiveTimeoutsPerRule() {
		Map<String, Object> snapshot = diagnostics.snapshot();
		Map<String, Object> rulesNode = (Map<String, Object>) snapshot.get("rules");
		assertEquals(2, rulesNode.get("enabled"));
		assertEquals(2, rulesNode.get("different"));

		List<Map<String, Object>> items = (List<Map<String, Object>>) rulesNode.get("items");
		Map<String, Map<String, Object>> byRule = items.stream()
				.collect(Collectors.toMap(item -> (String) item.get("rule"), item -> item));

		Map<String, Object> enabled = byRule.get("GET https://api.example.com/.*");
		assertEquals(true, enabled.get("adaptiveEnabled"));
		assertEquals(10000L, enabled.get("configuredTimeoutMs"));
		assertEquals("rule", enabled.get("configuredSource"));
		assertEquals(20L, enabled.get("effectiveTimeoutMs"));
		assertEquals(-9980L, enabled.get("deltaMs"));
		assertEquals(true, enabled.get("differs"));

		Map<String, Object> disabled = byRule.get("GET https://api.example.com/stream/.*");
		assertEquals(false, disabled.get("adaptiveEnabled"));
		assertEquals(5000L, disabled.get("configuredTimeoutMs"));
		assertEquals(5000L, disabled.get("effectiveTimeoutMs"));
		assertNull(disabled.get("deltaMs"));
		assertEquals(false, disabled.get("differs"));

		Map<String, Object> inherited = byRule.get("GET https://api.example.com/inherit/.*");
		assertEquals(true, inherited.get("adaptiveEnabled"));
		assertEquals(30000L, inherited.get("configuredTimeoutMs"));
		assertEquals("default", inherited.get("configuredSource"));
		assertEquals(20L, inherited.get("effectiveTimeoutMs"));
		assertEquals(true, inherited.get("differs"));

		Map<String, Object> defaultRule = byRule.get(HttpClient5Config.DEFAULT_HOST_KEY);
		assertEquals(false, defaultRule.get("adaptiveEnabled"));
		assertEquals(30000L, defaultRule.get("configuredTimeoutMs"));
		assertEquals("rule", defaultRule.get("configuredSource"));
		assertEquals(false, defaultRule.get("differs"));
	}

	@Test
	void shouldReturnOnlyDifferingRules() {
		List<AdaptiveTimeoutDiagnostics.RuleSnapshot> differences = diagnostics.differences();

		assertEquals(2, differences.size());
		assertTrue(differences.stream().allMatch(AdaptiveTimeoutDiagnostics.RuleSnapshot::isDiffers));
		assertTrue(differences.stream().allMatch(AdaptiveTimeoutDiagnostics.RuleSnapshot::isAdaptiveEnabled));
	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldReportUnsetConfiguredTimeoutWhenNoDefaultRuleExists() {
		config.getRequestConfig().remove(HttpClient5Config.DEFAULT_HOST_KEY);
		AdaptiveTimeoutDiagnostics withoutDefault = new AdaptiveTimeoutDiagnostics(config, policy);

		Map<String, Object> snapshot = withoutDefault.snapshot();
		Map<String, Object> rulesNode = (Map<String, Object>) snapshot.get("rules");
		List<Map<String, Object>> items = (List<Map<String, Object>>) rulesNode.get("items");
		Map<String, Map<String, Object>> byRule = items.stream()
				.collect(Collectors.toMap(item -> (String) item.get("rule"), item -> item));

		Map<String, Object> inherited = byRule.get("GET https://api.example.com/inherit/.*");
		assertNull(inherited.get("configuredTimeoutMs"));
		assertEquals("unset", inherited.get("configuredSource"));
		assertEquals(20L, inherited.get("effectiveTimeoutMs"));
		assertEquals(true, inherited.get("differs"));
	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldReportStaleState() {
		clock.add(2, TimeUnit.MINUTES);
		policy.resolveTimeout("GET", "http://example.com/", propertiesEnabled());

		assertTrue(policy.isStale());
		Map<String, Object> snapshot = diagnostics.snapshot();
		assertEquals("STALE", snapshot.get("status"));
		assertEquals(true, snapshot.get("stale"));
	}

	@Test
	void shouldReportDisabledWhenNoRuleEnablesThePolicy() {
		HttpClient5Config disabledConfig = new HttpClient5Config();
		disabledConfig.setAutoconfig(false);
		RequestConfigProperties defaultRule = new RequestConfigProperties();
		defaultRule.setResponseTimeout(Timeout.ofSeconds(30));
		disabledConfig.getRequestConfig().put(HttpClient5Config.DEFAULT_HOST_KEY, defaultRule);
		MicrometerAdaptiveTimeoutPolicy disabledPolicy = new MicrometerAdaptiveTimeoutPolicy(disabledConfig, clock);
		AdaptiveTimeoutDiagnostics disabledDiagnostics = new AdaptiveTimeoutDiagnostics(disabledConfig, disabledPolicy);

		assertEquals("DISABLED", disabledDiagnostics.snapshot().get("status"));
		assertFalse(disabledPolicy.isReady());
	}

	private static RequestConfigProperties propertiesEnabled() {
		RequestConfigProperties properties = new RequestConfigProperties();
		properties.setAdaptiveTimeout(AdaptiveTimeoutRuleProperties.builder().enabled(true).build());
		return properties;
	}
}
