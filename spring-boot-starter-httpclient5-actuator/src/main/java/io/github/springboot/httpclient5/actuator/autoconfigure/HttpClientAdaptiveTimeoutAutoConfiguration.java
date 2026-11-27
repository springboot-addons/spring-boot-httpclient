package io.github.springboot.httpclient5.actuator.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.springboot.httpclient5.actuator.adaptive.AdaptiveTimeoutDiagnostics;
import io.github.springboot.httpclient5.actuator.adaptive.AdaptiveTimeoutDiagnosticsLogger;
import io.github.springboot.httpclient5.actuator.adaptive.AdaptiveTimeoutEndpoint;
import io.github.springboot.httpclient5.actuator.adaptive.AdaptiveTimeoutExecChainHandler;
import io.github.springboot.httpclient5.actuator.adaptive.MicrometerAdaptiveTimeoutPolicy;
import io.github.springboot.httpclient5.core.HttpClient5AutoConfiguration;
import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Wires the optional adaptive response timeout policy and its metrics.
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
	public AdaptiveTimeoutExecChainHandler adaptiveTimeoutExecChainHandler(MicrometerAdaptiveTimeoutPolicy policy,
			Clock clock) {
		return new AdaptiveTimeoutExecChainHandler(policy, clock);
	}

	@Bean
	public AdaptiveTimeoutDiagnostics adaptiveTimeoutDiagnostics(HttpClient5Config config,
			MicrometerAdaptiveTimeoutPolicy policy) {
		return new AdaptiveTimeoutDiagnostics(config, policy);
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.httpclient5.adaptive-timeout.diagnostics", name = "log-enabled",
			havingValue = "true", matchIfMissing = true)
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
	public MeterBinder adaptiveTimeoutMetrics(MicrometerAdaptiveTimeoutPolicy policy) {
		return registry -> {
			Gauge.builder("httpclient5.adaptive.timeout", policy, p -> p.getCurrentTimeoutSeconds())
					.description("Current adaptive response timeout, in seconds")
					.register(registry);
			Gauge.builder("httpclient5.adaptive.percentile", policy, p -> p.getCurrentPercentileSeconds())
					.description("Current response-time percentile used by the adaptive timeout, in seconds")
					.register(registry);
			Gauge.builder("httpclient5.adaptive.samples", policy, p -> p.getCurrentSamples())
					.description("Number of samples used by the last adaptive timeout computation")
					.register(registry);
			FunctionCounter.builder("httpclient5.adaptive.timeouts", policy, p -> (double) p.getTimeoutCount())
					.description("Number of response timeouts recorded by the adaptive timeout policy")
					.register(registry);
		};
	}
}
