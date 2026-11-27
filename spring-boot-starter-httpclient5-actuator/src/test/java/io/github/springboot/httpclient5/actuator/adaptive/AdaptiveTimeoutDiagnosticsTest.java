package io.github.springboot.httpclient5.actuator.adaptive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
	private AdaptiveTimeoutRuleTracker tracker;
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

		tracker = new AdaptiveTimeoutRuleTracker(clock);
		policy = new MicrometerAdaptiveTimeoutPolicy(config, clock);
		policy.record(TimeValue.ofMilliseconds(10).toNanoseconds());
		policy.resolveTimeout("GET", "http://example.com/", propertiesEnabled());
		diagnostics = new AdaptiveTimeoutDiagnostics(config, policy, tracker);
	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldExposeGlobalStateAndCurrentValues() {
		Map<String, Object> snapshot = diagnostics.snapshot();

		assertEquals("READY", snapshot.get("status"));
		assertEquals(false, snapshot.get("globalEnabled"));
		assertEquals(false, snapshot.get("dryRun"));
		assertEquals(true, snapshot.get("ready"));
		assertEquals(false, snapshot.get("stale"));

		Map<String, Object> current = (Map<String, Object>) snapshot.get("current");
		assertEquals(20L, current.get("timeoutMs"));
		assertEquals(10L, current.get("percentileMs"));
		assertEquals(10L, current.get("p50Ms"));
		assertEquals(10L, current.get("p90Ms"));
		assertEquals(10L, current.get("p99Ms"));
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
		Map<String, Map<String, Object>> byRule = byRule(items);

		Map<String, Object> enabled = byRule.get("GET https://api.example.com/.*");
		assertEquals(true, enabled.get("adaptiveEnabled"));
		assertEquals(10000L, enabled.get("configuredTimeoutMs"));
		assertEquals("rule", enabled.get("configuredSource"));
		assertEquals(20L, enabled.get("candidateTimeoutMs"));
		assertEquals(20L, enabled.get("effectiveTimeoutMs"));
		assertEquals(-9980L, enabled.get("deltaMs"));
		assertEquals(true, enabled.get("differs"));
		assertEquals(true, enabled.get("applied"));
		assertEquals(0L, enabled.get("requests"));
		assertEquals(0L, enabled.get("timeouts"));
		assertEquals(0L, enabled.get("failures"));
		assertNull(enabled.get("lastSeenAt"));
		assertNull(enabled.get("lastEffectiveTimeoutMs"));
		assertEquals(false, enabled.get("lastTimedOut"));

		Map<String, Object> disabled = byRule.get("GET https://api.example.com/stream/.*");
		assertEquals(false, disabled.get("adaptiveEnabled"));
		assertEquals(5000L, disabled.get("configuredTimeoutMs"));
		assertEquals(5000L, disabled.get("effectiveTimeoutMs"));
		assertNull(disabled.get("candidateTimeoutMs"));
		assertNull(disabled.get("deltaMs"));
		assertEquals(false, disabled.get("differs"));
		assertEquals(false, disabled.get("applied"));

		Map<String, Object> inherited = byRule.get("GET https://api.example.com/inherit/.*");
		assertEquals(true, inherited.get("adaptiveEnabled"));
		assertEquals(30000L, inherited.get("configuredTimeoutMs"));
		assertEquals("default", inherited.get("configuredSource"));
		assertEquals(20L, inherited.get("candidateTimeoutMs"));
		assertEquals(20L, inherited.get("effectiveTimeoutMs"));
		assertEquals(true, inherited.get("differs"));
		assertEquals(true, inherited.get("applied"));

		Map<String, Object> defaultRule = byRule.get(HttpClient5Config.DEFAULT_HOST_KEY);
		assertEquals(false, defaultRule.get("adaptiveEnabled"));
		assertEquals(30000L, defaultRule.get("configuredTimeoutMs"));
		assertEquals("rule", defaultRule.get("configuredSource"));
		assertEquals(false, defaultRule.get("differs"));
		assertEquals(false, defaultRule.get("applied"));
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
		AdaptiveTimeoutDiagnostics withoutDefault = new AdaptiveTimeoutDiagnostics(config, policy, tracker);

		Map<String, Object> snapshot = withoutDefault.snapshot();
		Map<String, Object> rulesNode = (Map<String, Object>) snapshot.get("rules");
		List<Map<String, Object>> items = (List<Map<String, Object>>) rulesNode.get("items");
		Map<String, Map<String, Object>> byRule = byRule(items);

		Map<String, Object> inherited = byRule.get("GET https://api.example.com/inherit/.*");
		assertNull(inherited.get("configuredTimeoutMs"));
		assertEquals("unset", inherited.get("configuredSource"));
		assertEquals(20L, inherited.get("candidateTimeoutMs"));
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
		AdaptiveTimeoutDiagnostics disabledDiagnostics = new AdaptiveTimeoutDiagnostics(disabledConfig, disabledPolicy,
				tracker);

		assertEquals("DISABLED", disabledDiagnostics.snapshot().get("status"));
		assertFalse(disabledPolicy.isReady());
	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldTrackPerRuleUsageAndLastRequest() {
		String rule = "GET https://api.example.com/.*";
		tracker.record(rule, Timeout.ofMilliseconds(20), false, false);
		tracker.record(rule, Timeout.ofMilliseconds(20), true, false);
		tracker.record(rule, Timeout.ofMilliseconds(20), false, true);

		Map<String, Object> snapshot = diagnostics.snapshot();
		Map<String, Object> rulesNode = (Map<String, Object>) snapshot.get("rules");
		List<Map<String, Object>> items = (List<Map<String, Object>>) rulesNode.get("items");
		Map<String, Object> enabled = byRule(items).get(rule);
		assertEquals(3L, enabled.get("requests"));
		assertEquals(1L, enabled.get("timeouts"));
		assertEquals(1L, enabled.get("failures"));
		assertNotNull(enabled.get("lastSeenAt"));
		assertEquals(20L, enabled.get("lastEffectiveTimeoutMs"));
		assertEquals(false, enabled.get("lastTimedOut"));

		Map<String, Object> lastRequest = (Map<String, Object>) snapshot.get("lastRequest");
		assertNotNull(lastRequest);
		assertEquals(rule, lastRequest.get("rule"));
		assertEquals(20L, lastRequest.get("effectiveTimeoutMs"));
		assertEquals(false, lastRequest.get("timedOut"));
		assertNotNull(lastRequest.get("at"));
	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldExposeCandidateButKeepConfiguredTimeoutInDryRun() {
		config.getAdaptiveTimeout().setDryRun(true);
		AdaptiveTimeoutDiagnostics dryRunDiagnostics = new AdaptiveTimeoutDiagnostics(config, policy, tracker);

		Map<String, Object> snapshot = dryRunDiagnostics.snapshot();
		assertEquals(true, snapshot.get("dryRun"));

		Map<String, Object> rulesNode = (Map<String, Object>) snapshot.get("rules");
		List<Map<String, Object>> items = (List<Map<String, Object>>) rulesNode.get("items");
		Map<String, Object> enabled = byRule(items).get("GET https://api.example.com/.*");
		assertEquals(20L, enabled.get("candidateTimeoutMs"));
		assertEquals(10000L, enabled.get("effectiveTimeoutMs"));
		assertEquals(false, enabled.get("applied"));
		assertEquals(true, enabled.get("differs"));
		assertEquals(-9980L, enabled.get("deltaMs"));
	}

	private static Map<String, Map<String, Object>> byRule(List<Map<String, Object>> items) {
		return items.stream().collect(Collectors.toMap(item -> (String) item.get("rule"), item -> item));
	}

	private static RequestConfigProperties propertiesEnabled() {
		RequestConfigProperties properties = new RequestConfigProperties();
		properties.setAdaptiveTimeout(AdaptiveTimeoutRuleProperties.builder().enabled(true).build());
		return properties;
	}
}
