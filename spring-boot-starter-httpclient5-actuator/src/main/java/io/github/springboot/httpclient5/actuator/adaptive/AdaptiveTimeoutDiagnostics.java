package io.github.springboot.httpclient5.actuator.adaptive;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.AdaptiveTimeoutProperties;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;

/**
 * Builds the runtime view of the adaptive timeout policy: the current computed values, the
 * configuration, and the per-rule comparison between the configured and effective timeouts.
 * <p>
 * The per-rule comparison is based on the <strong>declared</strong> {@code response-timeout} of the
 * rule or, when absent, on the {@code [default]} rule's declared value. It does not resolve the full
 * regex merge for a concrete request URI, which is why the source is reported explicitly.
 */
public class AdaptiveTimeoutDiagnostics {

	private final HttpClient5Config config;
	private final MicrometerAdaptiveTimeoutPolicy policy;

	public AdaptiveTimeoutDiagnostics(HttpClient5Config config, MicrometerAdaptiveTimeoutPolicy policy) {
		this.config = config;
		this.policy = policy;
	}

	public List<RuleSnapshot> rules() {
		AdaptiveTimeoutProperties global = config.getAdaptiveTimeout();
		boolean globalEnabled = global.isEnabled();
		Long adaptiveMillis = policy.isReady() ? policy.getCurrentTimeoutMillis() : null;
		RequestConfigProperties defaultRule = config.getRequestConfig()
				.get(HttpClient5Config.DEFAULT_HOST_KEY);
		Timeout defaultTimeout = defaultRule != null ? defaultRule.getResponseTimeout() : null;

		return config.getRequestConfig().entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(entry -> snapshotRule(entry.getKey(), entry.getValue(), globalEnabled, adaptiveMillis,
						defaultTimeout))
				.toList();
	}

	public List<RuleSnapshot> differences() {
		return rules().stream().filter(RuleSnapshot::isDiffers).toList();
	}

	public Map<String, Object> snapshot() {
		AdaptiveTimeoutProperties global = config.getAdaptiveTimeout();
		List<RuleSnapshot> rules = rules();
		int enabledRules = (int) rules.stream().filter(RuleSnapshot::isAdaptiveEnabled).count();
		int differentRules = (int) rules.stream().filter(RuleSnapshot::isDiffers).count();

		Map<String, Object> root = new LinkedHashMap<>();
		root.put("status", status(enabledRules));
		root.put("globalEnabled", global.isEnabled());
		root.put("ready", policy.isReady());
		root.put("stale", policy.isStale());
		root.put("lastComputedAt", policy.getLastComputedAtMillis() > 0
				? Instant.ofEpochMilli(policy.getLastComputedAtMillis()).toString()
				: null);
		root.put("ageMs", policy.getLastComputedAgeMillis() >= 0 ? policy.getLastComputedAgeMillis() : null);
		root.put("configuration", configuration(global));
		root.put("current", current(global));

		Map<String, Object> rulesNode = new LinkedHashMap<>();
		rulesNode.put("enabled", enabledRules);
		rulesNode.put("different", differentRules);
		rulesNode.put("items", rules.stream().map(this::toMap).toList());
		root.put("rules", rulesNode);
		return root;
	}

	private String status(int enabledRules) {
		if (enabledRules == 0) {
			return "DISABLED";
		}
		if (!policy.isReady()) {
			return "WARMING_UP";
		}
		if (policy.isStale()) {
			return "STALE";
		}
		return "READY";
	}

	private Map<String, Object> configuration(AdaptiveTimeoutProperties global) {
		Map<String, Object> configuration = new LinkedHashMap<>();
		configuration.put("percentile", global.getPercentile());
		configuration.put("factor", global.getFactor());
		configuration.put("lowerBoundFactor", global.getLowerBoundFactor());
		configuration.put("window", iso(global.getWindow()));
		configuration.put("refreshInterval", iso(global.getRefreshInterval()));
		configuration.put("warmupRequests", global.getWarmupRequests());
		configuration.put("minSamples", global.getMinSamples());
		configuration.put("maxSamples", global.getMaxSamples());
		configuration.put("minTimeoutMs", global.getMinTimeout() != null
				? global.getMinTimeout().toMilliseconds()
				: null);
		configuration.put("maxTimeoutMs", global.getMaxTimeout() != null
				? global.getMaxTimeout().toMilliseconds()
				: null);
		configuration.put("includeTimeouts", global.isIncludeTimeouts());
		configuration.put("diagnostics", diagnostics(global.getDiagnostics()));
		return configuration;
	}

	private Map<String, Object> diagnostics(AdaptiveTimeoutProperties.DiagnosticsProperties diagnostics) {
		Map<String, Object> node = new LinkedHashMap<>();
		if (diagnostics != null) {
			node.put("logEnabled", diagnostics.isLogEnabled());
			node.put("logInterval", iso(diagnostics.getLogInterval()));
			node.put("logMaxRules", diagnostics.getLogMaxRules());
		}
		return node;
	}

	private Map<String, Object> current(AdaptiveTimeoutProperties global) {
		Map<String, Object> current = new LinkedHashMap<>();
		boolean ready = policy.isReady();
		current.put("timeoutMs", ready ? policy.getCurrentTimeoutMillis() : null);
		current.put("percentileMs", ready ? policy.getCurrentPercentileMillis() : null);
		current.put("minObservedMs", ready ? policy.getCurrentMinObservedMillis() : null);
		current.put("samples", policy.getCurrentSamples());
		current.put("totalSamples", policy.getTotalSamples());
		current.put("timeouts", policy.getTimeoutCount());
		current.put("timeoutRate", policy.getTotalSamples() > 0
				? (double) policy.getTimeoutCount() / policy.getTotalSamples()
				: 0.0d);
		current.put("warmupRequests", global.getWarmupRequests());
		current.put("minSamples", global.getMinSamples());
		current.put("warmupProgress", global.getWarmupRequests() > 0
				? Math.min(1.0d, (double) policy.getTotalSamples() / global.getWarmupRequests())
				: 1.0d);
		return current;
	}

	private RuleSnapshot snapshotRule(String ruleKey, RequestConfigProperties rule, boolean globalEnabled,
			Long adaptiveMillis, Timeout defaultTimeout) {
		Boolean ruleEnabled = rule.getAdaptiveTimeout() != null ? rule.getAdaptiveTimeout().getEnabled() : null;
		boolean adaptiveEnabled = ruleEnabled != null ? ruleEnabled : globalEnabled;

		Timeout declared = rule.getResponseTimeout();
		Timeout configured;
		String configuredSource;
		if (declared != null) {
			configured = declared;
			configuredSource = "rule";
		} else if (!HttpClient5Config.DEFAULT_HOST_KEY.equals(ruleKey) && defaultTimeout != null) {
			configured = defaultTimeout;
			configuredSource = "default";
		} else {
			configured = null;
			configuredSource = "unset";
		}

		Long configuredMillis = configured != null ? configured.toMilliseconds() : null;
		Long effectiveMillis = adaptiveEnabled && adaptiveMillis != null ? adaptiveMillis : configuredMillis;
		Long deltaMillis = adaptiveEnabled && adaptiveMillis != null && configuredMillis != null
				? adaptiveMillis - configuredMillis
				: null;
		boolean differs = adaptiveEnabled && adaptiveMillis != null
				&& (configuredMillis == null || !configuredMillis.equals(adaptiveMillis));
		return new RuleSnapshot(ruleKey, adaptiveEnabled, configuredMillis, configuredSource, effectiveMillis,
				deltaMillis, differs);
	}

	private Map<String, Object> toMap(RuleSnapshot rule) {
		Map<String, Object> node = new LinkedHashMap<>();
		node.put("rule", rule.getRule());
		node.put("adaptiveEnabled", rule.isAdaptiveEnabled());
		node.put("configuredTimeoutMs", rule.getConfiguredMillis());
		node.put("configuredSource", rule.getConfiguredSource());
		node.put("effectiveTimeoutMs", rule.getEffectiveMillis());
		node.put("deltaMs", rule.getDeltaMillis());
		node.put("differs", rule.isDiffers());
		return node;
	}

	private static String iso(TimeValue value) {
		return value != null ? Duration.ofNanos(value.toNanoseconds()).toString() : null;
	}

	public static final class RuleSnapshot {

		private final String rule;
		private final boolean adaptiveEnabled;
		private final Long configuredMillis;
		private final String configuredSource;
		private final Long effectiveMillis;
		private final Long deltaMillis;
		private final boolean differs;

		RuleSnapshot(String rule, boolean adaptiveEnabled, Long configuredMillis, String configuredSource,
				Long effectiveMillis, Long deltaMillis, boolean differs) {
			this.rule = rule;
			this.adaptiveEnabled = adaptiveEnabled;
			this.configuredMillis = configuredMillis;
			this.configuredSource = configuredSource;
			this.effectiveMillis = effectiveMillis;
			this.deltaMillis = deltaMillis;
			this.differs = differs;
		}

		public String getRule() {
			return rule;
		}

		public boolean isAdaptiveEnabled() {
			return adaptiveEnabled;
		}

		public Long getConfiguredMillis() {
			return configuredMillis;
		}

		public String getConfiguredSource() {
			return configuredSource;
		}

		public Long getEffectiveMillis() {
			return effectiveMillis;
		}

		public Long getDeltaMillis() {
			return deltaMillis;
		}

		public boolean isDiffers() {
			return differs;
		}
	}
}
