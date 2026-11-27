package io.github.springboot.httpclient5.actuator.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.springboot.httpclient5.actuator.adaptive.AdaptiveTimeoutDiagnostics;
import io.github.springboot.httpclient5.actuator.adaptive.AdaptiveTimeoutDiagnosticsLogger;
import io.github.springboot.httpclient5.actuator.adaptive.AdaptiveTimeoutEndpoint;
import io.github.springboot.httpclient5.actuator.adaptive.AdaptiveTimeoutExecChainHandler;
import io.github.springboot.httpclient5.actuator.adaptive.AdaptiveTimeoutRuleTracker;
import io.github.springboot.httpclient5.actuator.adaptive.MicrometerAdaptiveTimeoutPolicy;
import io.github.springboot.httpclient5.core.HttpClient5AutoConfiguration;
import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Wires the optional adaptive response timeout policy and its diagnostics.
 * <p>
 * The policy is created unconditionally: the global switch and the per-rule overrides are
 * evaluated per request. This keeps the sampling window warm even before the policy is enabled.
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(HttpClient5AutoConfiguration.class)
public class HttpClientAdaptiveTimeoutAutoConfiguration {

	@Bean
	public MicrometerAdaptiveTimeoutPolicy adaptiveTimeoutPolicy(HttpClient5Config config, Clock clock) {
		return new MicrometerAdaptiveTimeoutPolicy(config, clock);
	}

	@Bean
	public AdaptiveTimeoutRuleTracker adaptiveTimeoutRuleTracker(Clock clock) {
		return new AdaptiveTimeoutRuleTracker(clock);
	}

	@Bean
	public AdaptiveTimeoutExecChainHandler adaptiveTimeoutExecChainHandler(MicrometerAdaptiveTimeoutPolicy policy,
			AdaptiveTimeoutRuleTracker tracker, Clock clock) {
		return new AdaptiveTimeoutExecChainHandler(policy, tracker, clock);
	}

	@Bean
	public AdaptiveTimeoutDiagnostics adaptiveTimeoutDiagnostics(HttpClient5Config config,
			MicrometerAdaptiveTimeoutPolicy policy, AdaptiveTimeoutRuleTracker tracker) {
		return new AdaptiveTimeoutDiagnostics(config, policy, tracker);
	}

	@Bean
	@ConditionalOnExpression("${spring.httpclient5.adaptive-timeout.diagnostics.log-enabled:true} or ${spring.httpclient5.adaptive-timeout.diagnostics.change-log-enabled:true}")
	public AdaptiveTimeoutDiagnosticsLogger adaptiveTimeoutDiagnosticsLogger(
			AdaptiveTimeoutDiagnostics diagnostics, HttpClient5Config config,
			MicrometerAdaptiveTimeoutPolicy policy) {
		return new AdaptiveTimeoutDiagnosticsLogger(diagnostics, config, policy);
	}

	@Bean
	@ConditionalOnProperty(prefix = "management.endpoint.httpclient5-adaptive-timeout", name = "enabled",
			havingValue = "true", matchIfMissing = true)
	public AdaptiveTimeoutEndpoint adaptiveTimeoutEndpoint(AdaptiveTimeoutDiagnostics diagnostics) {
		return new AdaptiveTimeoutEndpoint(diagnostics);
	}

	@Bean
	public MeterBinder adaptiveTimeoutMetrics(MicrometerAdaptiveTimeoutPolicy policy,
			AdaptiveTimeoutDiagnostics diagnostics) {
		return registry -> {
			Gauge.builder("httpclient5.adaptive.timeout", policy, p -> p.getCurrentTimeoutSeconds())
					.description("Current adaptive response timeout, in seconds")
					.register(registry);
			Gauge.builder("httpclient5.adaptive.percentile", policy, p -> p.getCurrentPercentileSeconds())
					.description("Current response-time percentile used by the adaptive timeout, in seconds")
					.register(registry);
			Gauge.builder("httpclient5.adaptive.p50", policy,
					p -> p.isReady() ? p.getCurrentP50Millis() / 1000d : Double.NaN)
					.description("Median TTFB used by the adaptive timeout, in seconds")
					.register(registry);
			Gauge.builder("httpclient5.adaptive.p90", policy,
					p -> p.isReady() ? p.getCurrentP90Millis() / 1000d : Double.NaN)
					.description("p90 TTFB used by the adaptive timeout, in seconds")
					.register(registry);
			Gauge.builder("httpclient5.adaptive.p99", policy,
					p -> p.isReady() ? p.getCurrentP99Millis() / 1000d : Double.NaN)
					.description("p99 TTFB used by the adaptive timeout, in seconds")
					.register(registry);
			Gauge.builder("httpclient5.adaptive.samples", policy, p -> p.getCurrentSamples())
					.description("Number of samples used by the last adaptive timeout computation")
					.register(registry);
			Gauge.builder("httpclient5.adaptive.status", diagnostics, d -> d.statusCode())
					.description("Adaptive timeout status: 0=DISABLED, 1=WARMING_UP, 2=READY, 3=STALE")
					.register(registry);
			Gauge.builder("httpclient5.adaptive.ready", diagnostics, d -> d.statusCode() == 2 ? 1 : 0)
					.description("1 when the adaptive timeout policy is READY, 0 otherwise")
					.register(registry);
			Gauge.builder("httpclient5.adaptive.stale", diagnostics, d -> d.statusCode() == 3 ? 1 : 0)
					.description("1 when the adaptive timeout policy is STALE, 0 otherwise")
					.register(registry);
			FunctionCounter.builder("httpclient5.adaptive.timeouts", policy, p -> (double) p.getTimeoutCount())
					.description("Number of response timeouts recorded by the adaptive timeout policy")
					.register(registry);
		};
	}

	@Bean
	public MeterBinder adaptiveTimeoutRuleMetrics(HttpClient5Config config,
			AdaptiveTimeoutDiagnostics diagnostics, AdaptiveTimeoutRuleTracker tracker) {
		return registry -> {
			for (String rule : config.getRequestConfig().keySet()) {
				Gauge.builder("httpclient5.adaptive.rule.configured.timeout", diagnostics,
						d -> d.configuredSeconds(rule))
						.tags("rule", rule)
						.description("Configured response timeout for the rule, in seconds")
						.register(registry);
				Gauge.builder("httpclient5.adaptive.rule.candidate.timeout", diagnostics,
						d -> d.candidateSeconds(rule))
						.tags("rule", rule)
						.description("Adaptive candidate timeout for the rule, in seconds")
						.register(registry);
				Gauge.builder("httpclient5.adaptive.rule.effective.timeout", diagnostics,
						d -> d.effectiveSeconds(rule))
						.tags("rule", rule)
						.description("Effective response timeout for the rule, in seconds")
						.register(registry);
				Gauge.builder("httpclient5.adaptive.rule.differs", diagnostics, d -> d.differs(rule))
						.tags("rule", rule)
						.description("1 when the candidate differs from the configured timeout for the rule")
						.register(registry);
				FunctionCounter.builder("httpclient5.adaptive.rule.requests", tracker,
						t -> (double) t.requests(rule))
						.tags("rule", rule)
						.description("Requests resolved for the rule")
						.register(registry);
				FunctionCounter.builder("httpclient5.adaptive.rule.timeouts", tracker,
						t -> (double) t.timeouts(rule))
						.tags("rule", rule)
						.description("Response timeouts observed for the rule")
						.register(registry);
				FunctionCounter.builder("httpclient5.adaptive.rule.failures", tracker,
						t -> (double) t.failures(rule))
						.tags("rule", rule)
						.description("Non-timeout failures observed for the rule")
						.register(registry);
			}
		};
	}
}
