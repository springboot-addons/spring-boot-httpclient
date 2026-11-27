package io.github.springboot.httpclient5.actuator.adaptive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.AdaptiveTimeoutProperties;
import io.github.springboot.httpclient5.core.config.model.AdaptiveTimeoutRuleProperties;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;
import io.micrometer.core.instrument.MockClock;

class MicrometerAdaptiveTimeoutPolicyTest {

	private MockClock clock;
	private MicrometerAdaptiveTimeoutPolicy policy;

	@BeforeEach
	void setUp() {
		clock = new MockClock();
		createPolicy(p -> {
		});
	}

	@Test
	void shouldStayInactiveBeforeWarmup() {
		createPolicy(p -> p.setWarmupRequests(3));
		record(10);
		record(10);

		assertNull(policy.resolveTimeout("GET", "http://example.com/", properties()));

		record(10);
		assertNotNull(policy.resolveTimeout("GET", "http://example.com/", properties()));
	}

	@Test
	void shouldComputePercentileWithSafetyFactor() {
		record(10);
		record(20);
		record(30);

		Timeout timeout = policy.resolveTimeout("GET", "http://example.com/", properties());

		assertEquals(45, timeout.toMilliseconds());
	}

	@Test
	void shouldApplyLowerBoundOnMinimumObserved() {
		createPolicy(p -> p.setLowerBoundFactor(10.0d));
		record(10);
		record(10);
		record(10);

		Timeout timeout = policy.resolveTimeout("GET", "http://example.com/", properties());

		assertEquals(100, timeout.toMilliseconds());
	}

	@Test
	void shouldCapAtMaxTimeout() {
		createPolicy(p -> {
			p.setMaxTimeout(Timeout.ofMilliseconds(12));
			p.setLowerBoundFactor(1.0d);
		});
		record(10);

		Timeout timeout = policy.resolveTimeout("GET", "http://example.com/", properties());

		assertEquals(12, timeout.toMilliseconds());
	}

	@Test
	void shouldIgnoreSamplesOutsideWindow() {
		record(10);
		clock.add(2, TimeUnit.MINUTES);
		record(100);

		Timeout timeout = policy.resolveTimeout("GET", "http://example.com/", properties());

		assertEquals(200, timeout.toMilliseconds());
	}

	@Test
	void shouldKeepLastValueWhenWindowIsEmpty() {
		record(10);
		assertEquals(20, policy.resolveTimeout("GET", "http://example.com/", properties()).toMilliseconds());

		clock.add(2, TimeUnit.MINUTES);
		Timeout kept = policy.resolveTimeout("GET", "http://example.com/", properties());

		assertEquals(20, kept.toMilliseconds());
		assertTrue(policy.isStale());
	}

	@Test
	void shouldRespectPerRuleDisable() {
		record(10);

		assertNull(policy.resolveTimeout("GET", "http://example.com/", properties(Boolean.FALSE)));
	}

	@Test
	void shouldEnablePerRuleWhenGlobalDisabled() {
		createPolicy(p -> p.setEnabled(false));
		record(10);

		assertNull(policy.resolveTimeout("GET", "http://example.com/", properties()));
		assertEquals(20, policy.resolveTimeout("GET", "http://example.com/", properties(Boolean.TRUE)).toMilliseconds());
	}

	@Test
	void shouldRecordTimeoutAsCensoredSample() {
		policy.recordTimeout(Timeout.ofMilliseconds(100));

		Timeout timeout = policy.resolveTimeout("GET", "http://example.com/", properties());

		assertEquals(200, timeout.toMilliseconds());
		assertEquals(1, policy.getTimeoutCount());
		assertEquals(1, policy.getTotalSamples());
	}

	@Test
	void shouldNotRecordTimeoutWhenDisabled() {
		createPolicy(p -> p.setIncludeTimeouts(false));
		policy.recordTimeout(Timeout.ofMilliseconds(100));

		assertEquals(0, policy.getTotalSamples());
		assertEquals(1, policy.getTimeoutCount());
		assertNull(policy.resolveTimeout("GET", "http://example.com/", properties()));
	}

	@Test
	void shouldExposeMetricValues() {
		record(10);
		policy.resolveTimeout("GET", "http://example.com/", properties());

		assertEquals(0.02d, policy.getCurrentTimeoutSeconds(), 0.0001d);
		assertEquals(0.01d, policy.getCurrentPercentileSeconds(), 0.0001d);
		assertEquals(1, policy.getCurrentSamples());
		assertEquals(1, policy.getWindowSamples());
		assertTrue(policy.isReady());
		assertFalse(policy.isStale());
		assertEquals(20L, policy.getCurrentTimeoutMillis());
		assertEquals(10L, policy.getCurrentPercentileMillis());
		assertEquals(10L, policy.getCurrentMinObservedMillis());
		assertTrue(policy.getLastComputedAtMillis() > 0L);
		assertTrue(policy.getLastComputedAgeMillis() >= 0L);
	}

	@Test
	void shouldRejectInvalidPercentile() {
		assertThrows(IllegalArgumentException.class, () -> createPolicy(p -> p.setPercentile(1.5d)));
	}

	@Test
	void shouldRejectMinSamplesGreaterThanMaxSamples() {
		assertThrows(IllegalArgumentException.class, () -> createPolicy(p -> {
			p.setMaxSamples(10);
			p.setMinSamples(11);
		}));
	}

	@Test
	void shouldRejectNonPositiveDiagnosticsLogInterval() {
		assertThrows(IllegalArgumentException.class,
				() -> createPolicy(p -> p.getDiagnostics().setLogInterval(TimeValue.ZERO_MILLISECONDS)));
	}

	@Test
	void shouldAllowZeroDiagnosticsIntervalWhenLoggingIsDisabled() {
		createPolicy(p -> {
			p.getDiagnostics().setLogEnabled(false);
			p.getDiagnostics().setLogInterval(TimeValue.ZERO_MILLISECONDS);
		});

		assertNotNull(policy);
	}

	private void createPolicy(Consumer<AdaptiveTimeoutProperties> customizer) {
		HttpClient5Config config = new HttpClient5Config();
		AdaptiveTimeoutProperties properties = config.getAdaptiveTimeout();
		properties.setEnabled(true);
		properties.setWarmupRequests(1);
		properties.setMinSamples(1);
		properties.setMaxSamples(10);
		properties.setWindow(TimeValue.ofMinutes(1));
		properties.setRefreshInterval(TimeValue.ZERO_MILLISECONDS);
		properties.setMinTimeout(Timeout.ZERO_MILLISECONDS);
		properties.setMaxTimeout(Timeout.ofSeconds(60));
		properties.setPercentile(0.99d);
		properties.setFactor(1.5d);
		properties.setLowerBoundFactor(2.0d);
		properties.setIncludeTimeouts(true);
		customizer.accept(properties);
		policy = new MicrometerAdaptiveTimeoutPolicy(config, clock);
	}

	private void record(long milliseconds) {
		policy.record(TimeValue.ofMilliseconds(milliseconds).toNanoseconds());
	}

	private static RequestConfigProperties properties() {
		return properties(null);
	}

	private static RequestConfigProperties properties(Boolean enabled) {
		RequestConfigProperties properties = new RequestConfigProperties();
		if (enabled != null) {
			properties.setAdaptiveTimeout(AdaptiveTimeoutRuleProperties.builder().enabled(enabled).build());
		}
		return properties;
	}
}
