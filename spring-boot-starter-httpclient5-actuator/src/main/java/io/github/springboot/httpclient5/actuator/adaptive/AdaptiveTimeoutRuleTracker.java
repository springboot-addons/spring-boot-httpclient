package io.github.springboot.httpclient5.actuator.adaptive;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.hc.core5.util.Timeout;

import io.micrometer.core.instrument.Clock;

/**
 * Tracks per-rule usage and the last resolved request for diagnostics.
 * <p>
 * The rule key comes from the core request configuration resolution. The tracker is updated once
 * per client execution attempt by the sampling handler.
 */
public class AdaptiveTimeoutRuleTracker {

	private final Clock clock;
	private final Map<String, RuleStats> stats = new ConcurrentHashMap<>();
	private final AtomicReference<LastRequest> lastRequest = new AtomicReference<>();
	private final AtomicLong version = new AtomicLong();

	public AdaptiveTimeoutRuleTracker(Clock clock) {
		this.clock = clock;
	}

	public void record(String rule, Timeout effectiveTimeout, boolean timedOut, boolean failure) {
		if (rule == null || rule.isBlank()) {
			return;
		}
		long now = clock.wallTime();
		long timeoutMillis = effectiveTimeout != null ? effectiveTimeout.toMilliseconds() : -1L;
		RuleStats ruleStats = stats.computeIfAbsent(rule, key -> new RuleStats());
		ruleStats.requests.incrementAndGet();
		if (timedOut) {
			ruleStats.timeouts.incrementAndGet();
		}
		if (failure) {
			ruleStats.failures.incrementAndGet();
		}
		ruleStats.lastSeenMillis.set(now);
		ruleStats.lastEffectiveTimeoutMillis.set(timeoutMillis);
		ruleStats.lastTimedOut.set(timedOut);
		lastRequest.set(new LastRequest(rule, timeoutMillis, timedOut, now));
		version.incrementAndGet();
	}

	public long getVersion() {
		return version.get();
	}

	public long requests(String rule) {
		RuleStats ruleStats = stats.get(rule);
		return ruleStats != null ? ruleStats.requests.get() : 0L;
	}

	public long timeouts(String rule) {
		RuleStats ruleStats = stats.get(rule);
		return ruleStats != null ? ruleStats.timeouts.get() : 0L;
	}

	public long failures(String rule) {
		RuleStats ruleStats = stats.get(rule);
		return ruleStats != null ? ruleStats.failures.get() : 0L;
	}

	public long lastSeenMillis(String rule) {
		RuleStats ruleStats = stats.get(rule);
		return ruleStats != null ? ruleStats.lastSeenMillis.get() : 0L;
	}

	public long lastEffectiveTimeoutMillis(String rule) {
		RuleStats ruleStats = stats.get(rule);
		return ruleStats != null ? ruleStats.lastEffectiveTimeoutMillis.get() : -1L;
	}

	public boolean lastTimedOut(String rule) {
		RuleStats ruleStats = stats.get(rule);
		return ruleStats != null && ruleStats.lastTimedOut.get();
	}

	public LastRequest lastRequest() {
		return lastRequest.get();
	}

	private static final class RuleStats {

		private final AtomicLong requests = new AtomicLong();
		private final AtomicLong timeouts = new AtomicLong();
		private final AtomicLong failures = new AtomicLong();
		private final AtomicLong lastSeenMillis = new AtomicLong();
		private final AtomicLong lastEffectiveTimeoutMillis = new AtomicLong(-1L);
		private final AtomicBoolean lastTimedOut = new AtomicBoolean();
	}

	public static final class LastRequest {

		private final String rule;
		private final long effectiveTimeoutMillis;
		private final boolean timedOut;
		private final long atMillis;

		LastRequest(String rule, long effectiveTimeoutMillis, boolean timedOut, long atMillis) {
			this.rule = rule;
			this.effectiveTimeoutMillis = effectiveTimeoutMillis;
			this.timedOut = timedOut;
			this.atMillis = atMillis;
		}

		public String getRule() {
			return rule;
		}

		public long getEffectiveTimeoutMillis() {
			return effectiveTimeoutMillis;
		}

		public boolean isTimedOut() {
			return timedOut;
		}

		public long getAtMillis() {
			return atMillis;
		}
	}
}
