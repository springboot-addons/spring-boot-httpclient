package io.github.springboot.httpclient5.actuator.adaptive;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.AdaptiveTimeoutProperties;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;

/**
 * Builds the runtime view of the adaptive timeout policy: the current computed values, the
 * configuration, the per-rule comparison between the configured and candidate timeouts, and the
 * per-rule usage statistics.
 * <p>
 * The per-rule comparison is based on the <strong>declared</strong> {@code response-timeout} of the
 * rule or, when absent, on the {@code [default]} rule's declared value. It does not resolve the
 * full regex merge for a concrete request URI, which is why the source is reported explicitly.
 */
public class AdaptiveTimeoutDiagnostics {

	private static final long CACHE_MILLIS = 1_000L;

	private final HttpClient5Config config;
	private final MicrometerAdaptiveTimeoutPolicy policy;
	private final AdaptiveTimeoutRuleTracker tracker;
	private volatile CachedRules cachedRules;

	public AdaptiveTimeoutDiagnostics(HttpClient5Config config, MicrometerAdaptiveTimeoutPolicy policy,
			AdaptiveTimeoutRuleTracker tracker) {
		this.config = config;
		this.policy = policy;
		this.tracker = tracker;
	}

	public List<RuleSnapshot> rules() {
		return currentCache().rules;
	}

	private CachedRules currentCache() {
		long now = System.currentTimeMillis();
		long policyVersion = policy.getStateVersion();
		long trackerVersion = tracker.getVersion();
		CachedRules cache = cachedRules;
		if (cache != null && now < cache.expiresAtMillis && cache.policyVersion == policyVersion
				&& cache.trackerVersion == trackerVersion) {
			return cache;
		}
		List<RuleSnapshot> rules = computeRules();
		Map<String, RuleSnapshot> byRule = rules.stream()
				.collect(Collectors.toMap(RuleSnapshot::getRule, rule -> rule));
		cache = new CachedRules(rules, byRule, now + CACHE_MILLIS, policyVersion, trackerVersion);
		cachedRules = cache;
		return cache;
	}

	public List<RuleSnapshot> differences() {
		return rules().stream().filter(RuleSnapshot::isDiffers).toList();
	}

	public String status() {
		int enabledRules = (int) rules().stream().filter(RuleSnapshot::isAdaptiveEnabled).count();
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

	public int statusCode() {
		return switch (status()) {
		case "DISABLED" -> 0;
		case "WARMING_UP" -> 1;
		case "READY" -> 2;
		case "STALE" -> 3;
		default -> -1;
		};
	}

	public RuleSnapshot rule(String ruleKey) {
		return currentCache().byRule.get(ruleKey);
	}

	public double configuredSeconds(String ruleKey) {
		RuleSnapshot rule = rule(ruleKey);
		return rule != null && rule.getConfiguredMillis() != null ? rule.getConfiguredMillis() / 1000d
				: Double.NaN;
	}

	public double candidateSeconds(String ruleKey) {
		RuleSnapshot rule = rule(ruleKey);
		return rule != null && rule.getCandidateMillis() != null ? rule.getCandidateMillis() / 1000d
				: Double.NaN;
	}

	public double effectiveSeconds(String ruleKey) {
		RuleSnapshot rule = rule(ruleKey);
		return rule != null && rule.getEffectiveMillis() != null ? rule.getEffectiveMillis() / 1000d
				: Double.NaN;
	}

	public double differs(String ruleKey) {
		RuleSnapshot rule = rule(ruleKey);
		return rule != null && rule.isDiffers() ? 1.0d : 0.0d;
	}

	public Map<String, Object> snapshot() {
		AdaptiveTimeoutProperties global = config.getAdaptiveTimeout();
		List<RuleSnapshot> rules = rules();
		int enabledRules = (int) rules.stream().filter(RuleSnapshot::isAdaptiveEnabled).count();
		int differentRules = (int) rules.stream().filter(RuleSnapshot::isDiffers).count();

		Map<String, Object> root = new LinkedHashMap<>();
		root.put("status", status());
		root.put("globalEnabled", global.isEnabled());
		root.put("dryRun", global.isDryRun());
		root.put("ready", policy.isReady());
		root.put("stale", policy.isStale());
		root.put("lastComputedAt", policy.getLastComputedAtMillis() > 0
				? Instant.ofEpochMilli(policy.getLastComputedAtMillis()).toString()
				: null);
		root.put("ageMs", policy.getLastComputedAgeMillis() >= 0 ? policy.getLastComputedAgeMillis() : null);
		root.put("configuration", configuration(global));
		root.put("current", current(global));
		root.put("lastRequest", lastRequest());

		Map<String, Object> rulesNode = new LinkedHashMap<>();
		rulesNode.put("enabled", enabledRules);
		rulesNode.put("different", differentRules);
		rulesNode.put("items", rules.stream().map(this::toMap).toList());
		root.put("rules", rulesNode);
		return root;
	}

	private List<RuleSnapshot> computeRules() {
		AdaptiveTimeoutProperties global = config.getAdaptiveTimeout();
		boolean globalEnabled = global.isEnabled();
		boolean dryRun = global.isDryRun();
		Long adaptiveMillis = policy.isReady() ? policy.getCurrentTimeoutMillis() : null;
		RequestConfigProperties defaultRule = config.getRequestConfig()
				.get(HttpClient5Config.DEFAULT_HOST_KEY);
		Timeout defaultTimeout = defaultRule != null ? defaultRule.getResponseTimeout() : null;

		return config.getRequestConfig().entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(entry -> snapshotRule(entry.getKey(), entry.getValue(), globalEnabled, dryRun, adaptiveMillis,
						defaultTimeout))
				.toList();
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
		configuration.put("dryRun", global.isDryRun());
		configuration.put("diagnostics", diagnostics(global.getDiagnostics()));
		return configuration;
	}

	private Map<String, Object> diagnostics(AdaptiveTimeoutProperties.DiagnosticsProperties diagnostics) {
		Map<String, Object> node = new LinkedHashMap<>();
		if (diagnostics != null) {
			node.put("logEnabled", diagnostics.isLogEnabled());
			node.put("changeLogEnabled", diagnostics.isChangeLogEnabled());
			node.put("changeLogThresholdPercent", diagnostics.getChangeLogThresholdPercent());
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
		current.put("p50Ms", ready ? policy.getCurrentP50Millis() : null);
		current.put("p90Ms", ready ? policy.getCurrentP90Millis() : null);
		current.put("p99Ms", ready ? policy.getCurrentP99Millis() : null);
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

	private Map<String, Object> lastRequest() {
		AdaptiveTimeoutRuleTracker.LastRequest last = tracker.lastRequest();
		if (last == null) {
			return null;
		}
		Map<String, Object> node = new LinkedHashMap<>();
		node.put("rule", last.getRule());
		node.put("effectiveTimeoutMs",
				last.getEffectiveTimeoutMillis() >= 0 ? last.getEffectiveTimeoutMillis() : null);
		node.put("timedOut", last.isTimedOut());
		node.put("at", Instant.ofEpochMilli(last.getAtMillis()).toString());
		return node;
	}

	private RuleSnapshot snapshotRule(String ruleKey, RequestConfigProperties rule, boolean globalEnabled,
			boolean dryRun, Long adaptiveMillis, Timeout defaultTimeout) {
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
		Long candidateMillis = adaptiveEnabled && adaptiveMillis != null ? adaptiveMillis : null;
		boolean applied = candidateMillis != null && !dryRun;
		Long effectiveMillis = applied ? candidateMillis : configuredMillis;
		Long deltaMillis = candidateMillis != null && configuredMillis != null
				? candidateMillis - configuredMillis
				: null;
		boolean differs = candidateMillis != null
				&& (configuredMillis == null || !configuredMillis.equals(candidateMillis));

		long requests = tracker.requests(ruleKey);
		long timeouts = tracker.timeouts(ruleKey);
		long failures = tracker.failures(ruleKey);
		long lastSeenMillis = tracker.lastSeenMillis(ruleKey);
		long lastEffectiveTimeoutMillis = tracker.lastEffectiveTimeoutMillis(ruleKey);
		boolean lastTimedOut = tracker.lastTimedOut(ruleKey);
		return new RuleSnapshot(ruleKey, adaptiveEnabled, configuredMillis, configuredSource, candidateMillis,
				effectiveMillis, deltaMillis, differs, applied, requests, timeouts, failures, lastSeenMillis,
				lastEffectiveTimeoutMillis, lastTimedOut);
	}

	private Map<String, Object> toMap(RuleSnapshot rule) {
		Map<String, Object> node = new LinkedHashMap<>();
		node.put("rule", rule.getRule());
		node.put("adaptiveEnabled", rule.isAdaptiveEnabled());
		node.put("configuredTimeoutMs", rule.getConfiguredMillis());
		node.put("configuredSource", rule.getConfiguredSource());
		node.put("candidateTimeoutMs", rule.getCandidateMillis());
		node.put("effectiveTimeoutMs", rule.getEffectiveMillis());
		node.put("deltaMs", rule.getDeltaMillis());
		node.put("differs", rule.isDiffers());
		node.put("applied", rule.isApplied());
		node.put("requests", rule.getRequests());
		node.put("timeouts", rule.getTimeouts());
		node.put("failures", rule.getFailures());
		node.put("lastSeenAt",
				rule.getLastSeenMillis() > 0 ? Instant.ofEpochMilli(rule.getLastSeenMillis()).toString() : null);
		node.put("lastEffectiveTimeoutMs",
				rule.getLastEffectiveTimeoutMillis() >= 0 ? rule.getLastEffectiveTimeoutMillis() : null);
		node.put("lastTimedOut", rule.isLastTimedOut());
		return node;
	}

	private static String iso(TimeValue value) {
		return value != null ? Duration.ofNanos(value.toNanoseconds()).toString() : null;
	}

	private static final class CachedRules {

		private final List<RuleSnapshot> rules;
		private final Map<String, RuleSnapshot> byRule;
		private final long expiresAtMillis;
		private final long policyVersion;
		private final long trackerVersion;

		private CachedRules(List<RuleSnapshot> rules, Map<String, RuleSnapshot> byRule, long expiresAtMillis,
				long policyVersion, long trackerVersion) {
			this.rules = rules;
			this.byRule = byRule;
			this.expiresAtMillis = expiresAtMillis;
			this.policyVersion = policyVersion;
			this.trackerVersion = trackerVersion;
		}
	}

	public static final class RuleSnapshot {

		private final String rule;
		private final boolean adaptiveEnabled;
		private final Long configuredMillis;
		private final String configuredSource;
		private final Long candidateMillis;
		private final Long effectiveMillis;
		private final Long deltaMillis;
		private final boolean differs;
		private final boolean applied;
		private final long requests;
		private final long timeouts;
		private final long failures;
		private final long lastSeenMillis;
		private final long lastEffectiveTimeoutMillis;
		private final boolean lastTimedOut;

		RuleSnapshot(String rule, boolean adaptiveEnabled, Long configuredMillis, String configuredSource,
				Long candidateMillis, Long effectiveMillis, Long deltaMillis, boolean differs, boolean applied,
				long requests, long timeouts, long failures, long lastSeenMillis,
				long lastEffectiveTimeoutMillis, boolean lastTimedOut) {
			this.rule = rule;
			this.adaptiveEnabled = adaptiveEnabled;
			this.configuredMillis = configuredMillis;
			this.configuredSource = configuredSource;
			this.candidateMillis = candidateMillis;
			this.effectiveMillis = effectiveMillis;
			this.deltaMillis = deltaMillis;
			this.differs = differs;
			this.applied = applied;
			this.requests = requests;
			this.timeouts = timeouts;
			this.failures = failures;
			this.lastSeenMillis = lastSeenMillis;
			this.lastEffectiveTimeoutMillis = lastEffectiveTimeoutMillis;
			this.lastTimedOut = lastTimedOut;
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

		public Long getCandidateMillis() {
			return candidateMillis;
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

		public boolean isApplied() {
			return applied;
		}

		public long getRequests() {
			return requests;
		}

		public long getTimeouts() {
			return timeouts;
		}

		public long getFailures() {
			return failures;
		}

		public long getLastSeenMillis() {
			return lastSeenMillis;
		}

		public long getLastEffectiveTimeoutMillis() {
			return lastEffectiveTimeoutMillis;
		}

		public boolean isLastTimedOut() {
			return lastTimedOut;
		}
	}
}
