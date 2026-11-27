package io.github.springboot.httpclient5.actuator.adaptive;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.AdaptiveTimeoutProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Periodically logs the rules whose effective adaptive timeout differs from their configured
 * timeout.
 * <p>
 * Only differences are logged, at DEBUG. The period is configurable with
 * {@code spring.httpclient5.adaptive-timeout.diagnostics.log-interval} (ISO-8601 duration, one
 * minute by default). The task is not registered when
 * {@code spring.httpclient5.adaptive-timeout.diagnostics.log-enabled=false}.
 */
@Slf4j
public class AdaptiveTimeoutDiagnosticsLogger {

	private final AdaptiveTimeoutDiagnostics diagnostics;
	private final HttpClient5Config config;
	private final MicrometerAdaptiveTimeoutPolicy policy;

	public AdaptiveTimeoutDiagnosticsLogger(AdaptiveTimeoutDiagnostics diagnostics, HttpClient5Config config,
			MicrometerAdaptiveTimeoutPolicy policy) {
		this.diagnostics = diagnostics;
		this.config = config;
		this.policy = policy;
	}

	@Scheduled(fixedDelayString = "${spring.httpclient5.adaptive-timeout.diagnostics.log-interval:PT1M}")
	public void logDifferences() {
		AdaptiveTimeoutProperties global = config.getAdaptiveTimeout();
		AdaptiveTimeoutProperties.DiagnosticsProperties diagnosticsProperties = global.getDiagnostics();
		if (diagnosticsProperties == null || !diagnosticsProperties.isLogEnabled() || !policy.isReady()) {
			return;
		}
		List<AdaptiveTimeoutDiagnostics.RuleSnapshot> differences = diagnostics.differences();
		if (differences.isEmpty()) {
			return;
		}

		int max = Math.max(1, diagnosticsProperties.getLogMaxRules());
		int logged = 0;
		for (AdaptiveTimeoutDiagnostics.RuleSnapshot difference : differences) {
			if (logged >= max) {
				break;
			}
			log.debug(
					"Adaptive timeout differs (samples={}, p{}={}ms, stale={}): rule='{}' configured={} source={} effective={} delta={}",
					policy.getCurrentSamples(),
					global.getPercentile(),
					policy.getCurrentPercentileMillis(),
					policy.isStale(),
					difference.getRule(),
					formatMillis(difference.getConfiguredMillis()),
					difference.getConfiguredSource(),
					formatMillis(difference.getEffectiveMillis()),
					formatMillis(difference.getDeltaMillis()));
			logged++;
		}
		if (differences.size() > max) {
			log.debug("Adaptive timeout differs for {} more rule(s)", differences.size() - max);
		}
	}

	private static String formatMillis(Long value) {
		return value != null ? value + "ms" : "unset";
	}
}
