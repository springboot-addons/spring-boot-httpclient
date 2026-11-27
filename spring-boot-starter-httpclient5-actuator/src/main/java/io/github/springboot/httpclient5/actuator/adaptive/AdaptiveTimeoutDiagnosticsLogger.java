package io.github.springboot.httpclient5.actuator.adaptive;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.AdaptiveTimeoutProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Periodically logs the rules whose effective adaptive timeout differs from their configured
 * timeout, and emits INFO events when the policy becomes STALE or when the computed timeout changes
 * significantly.
 * <p>
 * Only differences are logged, at DEBUG. The period is configurable with
 * {@code spring.httpclient5.adaptive-timeout.diagnostics.log-interval} (ISO-8601 duration, one
 * minute by default). The task is not registered when both {@code log-enabled} and
 * {@code change-log-enabled} are false.
 */
@Slf4j
public class AdaptiveTimeoutDiagnosticsLogger {

	private final AdaptiveTimeoutDiagnostics diagnostics;
	private final HttpClient5Config config;
	private final MicrometerAdaptiveTimeoutPolicy policy;
	private Long lastTimeoutMillis;
	private String lastStatus;

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
		if (diagnosticsProperties == null) {
			return;
		}
		boolean logEnabled = diagnosticsProperties.isLogEnabled();
		boolean changeLogEnabled = diagnosticsProperties.isChangeLogEnabled();
		if (!logEnabled && !changeLogEnabled) {
			return;
		}

		if (changeLogEnabled) {
			logChanges(diagnosticsProperties);
		}
		if (!logEnabled || !policy.isReady()) {
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
					"Adaptive timeout differs (dryRun={}, samples={}, p99={}ms, stale={}): rule='{}' configured={} source={} candidate={} effective={} delta={} applied={} requests={} timeouts={}",
					global.isDryRun(),
					policy.getCurrentSamples(),
					policy.getCurrentP99Millis(),
					policy.isStale(),
					difference.getRule(),
					formatMillis(difference.getConfiguredMillis()),
					difference.getConfiguredSource(),
					formatMillis(difference.getCandidateMillis()),
					formatMillis(difference.getEffectiveMillis()),
					formatMillis(difference.getDeltaMillis()),
					difference.isApplied(),
					difference.getRequests(),
					difference.getTimeouts());
			logged++;
		}
		if (differences.size() > max) {
			log.debug("Adaptive timeout differs for {} more rule(s)", differences.size() - max);
		}
	}

	private void logChanges(AdaptiveTimeoutProperties.DiagnosticsProperties diagnosticsProperties) {
		String status = diagnostics.status();
		long currentTimeout = policy.getCurrentTimeoutMillis();
		if (lastStatus != null && !"STALE".equals(lastStatus) && "STALE".equals(status)) {
			log.info("Adaptive response timeout policy became STALE (last computed {} ms ago)",
					policy.getLastComputedAgeMillis());
		}
		if (lastTimeoutMillis != null && lastTimeoutMillis > 0 && currentTimeout > 0) {
			double change = Math.abs(currentTimeout - lastTimeoutMillis) * 100.0d / lastTimeoutMillis;
			if (change >= diagnosticsProperties.getChangeLogThresholdPercent()) {
				log.info(
						"Adaptive response timeout changed by {}% ({}ms -> {}ms, p99={}ms, samples={})",
						round(change), lastTimeoutMillis, currentTimeout, policy.getCurrentP99Millis(),
						policy.getCurrentSamples());
			}
		}
		lastStatus = status;
		if (currentTimeout > 0) {
			lastTimeoutMillis = currentTimeout;
		}
	}

	private static double round(double value) {
		return Math.round(value * 10.0d) / 10.0d;
	}

	private static String formatMillis(Long value) {
		return value != null ? value + "ms" : "unset";
	}
}
