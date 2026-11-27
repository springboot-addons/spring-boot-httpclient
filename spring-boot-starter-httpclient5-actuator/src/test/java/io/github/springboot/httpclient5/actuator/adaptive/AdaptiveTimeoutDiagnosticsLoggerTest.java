package io.github.springboot.httpclient5.actuator.adaptive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.AdaptiveTimeoutProperties;
import io.github.springboot.httpclient5.core.config.model.AdaptiveTimeoutRuleProperties;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;
import io.micrometer.core.instrument.MockClock;

class AdaptiveTimeoutDiagnosticsLoggerTest {

	private static final String ENABLED_RULE = "GET https://api.example.com/.*";
	private static final String DISABLED_RULE = "GET https://api.example.com/stream/.*";

	private MockClock clock;
	private HttpClient5Config config;
	private MicrometerAdaptiveTimeoutPolicy policy;
	private AdaptiveTimeoutDiagnostics diagnostics;
	private AdaptiveTimeoutDiagnosticsLogger logger;
	private Logger logbackLogger;
	private Level originalLevel;
	private ListAppender<ILoggingEvent> appender;

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
		config.getRequestConfig().put(ENABLED_RULE, enabledRule);

		RequestConfigProperties disabledRule = new RequestConfigProperties();
		disabledRule.setResponseTimeout(Timeout.ofSeconds(5));
		disabledRule.setAdaptiveTimeout(AdaptiveTimeoutRuleProperties.builder().enabled(false).build());
		config.getRequestConfig().put(DISABLED_RULE, disabledRule);

		policy = new MicrometerAdaptiveTimeoutPolicy(config, clock);
		policy.record(TimeValue.ofMilliseconds(10).toNanoseconds());
		policy.resolveTimeout("GET", "http://example.com/", propertiesEnabled());
		diagnostics = new AdaptiveTimeoutDiagnostics(config, policy);
		logger = new AdaptiveTimeoutDiagnosticsLogger(diagnostics, config, policy);

		logbackLogger = (Logger) LoggerFactory.getLogger(AdaptiveTimeoutDiagnosticsLogger.class);
		originalLevel = logbackLogger.getLevel();
		logbackLogger.setLevel(Level.DEBUG);
		appender = new ListAppender<>();
		appender.start();
		logbackLogger.addAppender(appender);
	}

	@AfterEach
	void tearDown() {
		logbackLogger.detachAppender(appender);
		logbackLogger.setLevel(originalLevel);
	}

	@Test
	void shouldLogOnlyDifferingRules() {
		logger.logDifferences();

		assertEquals(1, appender.list.size());
		String message = appender.list.get(0).getFormattedMessage();
		assertTrue(message.contains(ENABLED_RULE));
		assertTrue(message.contains("configured=10000ms"));
		assertTrue(message.contains("effective=20ms"));
		assertTrue(message.contains("delta=-9980ms"));
		assertFalse(message.contains(DISABLED_RULE));
	}

	@Test
	void shouldNotLogWhenPolicyIsNotReady() {
		HttpClient5Config emptyConfig = new HttpClient5Config();
		emptyConfig.getAdaptiveTimeout().setEnabled(true);
		emptyConfig.setAutoconfig(false);
		RequestConfigProperties rule = new RequestConfigProperties();
		rule.setResponseTimeout(Timeout.ofSeconds(10));
		emptyConfig.getRequestConfig().put("GET https://api.example.com/.*", rule);
		MicrometerAdaptiveTimeoutPolicy notReadyPolicy = new MicrometerAdaptiveTimeoutPolicy(emptyConfig, clock);
		AdaptiveTimeoutDiagnostics notReadyDiagnostics = new AdaptiveTimeoutDiagnostics(emptyConfig, notReadyPolicy);
		AdaptiveTimeoutDiagnosticsLogger notReadyLogger = new AdaptiveTimeoutDiagnosticsLogger(notReadyDiagnostics,
				emptyConfig, notReadyPolicy);

		notReadyLogger.logDifferences();

		assertTrue(appender.list.isEmpty());
	}

	@Test
	void shouldRespectLogMaxRules() {
		config.getAdaptiveTimeout().getDiagnostics().setLogMaxRules(1);
		RequestConfigProperties inheritedRule = new RequestConfigProperties();
		inheritedRule.setAdaptiveTimeout(AdaptiveTimeoutRuleProperties.builder().enabled(true).build());
		config.getRequestConfig().put("GET https://api.example.com/inherit/.*", inheritedRule);

		logger.logDifferences();

		assertEquals(2, appender.list.size());
		assertTrue(appender.list.get(0).getFormattedMessage().contains(ENABLED_RULE)
				|| appender.list.get(0).getFormattedMessage().contains("GET https://api.example.com/inherit/.*"));
		assertTrue(appender.list.get(1).getFormattedMessage().contains("1 more rule(s)"));
	}

	@Test
	void shouldNotLogWhenLoggingIsDisabled() {
		config.getAdaptiveTimeout().getDiagnostics().setLogEnabled(false);

		logger.logDifferences();

		assertTrue(appender.list.isEmpty());
	}

	private static RequestConfigProperties propertiesEnabled() {
		RequestConfigProperties properties = new RequestConfigProperties();
		properties.setAdaptiveTimeout(AdaptiveTimeoutRuleProperties.builder().enabled(true).build());
		return properties;
	}
}
