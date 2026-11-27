package io.github.springboot.httpclient5.actuator.adaptive;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.hc.core5.util.Timeout;

import io.github.springboot.httpclient5.core.config.AdaptiveTimeoutPolicy;
import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.AdaptiveTimeoutProperties;
import io.github.springboot.httpclient5.core.config.model.AdaptiveTimeoutRuleProperties;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;
import io.micrometer.core.instrument.Clock;

/**
 * Adaptive response timeout policy fed by the time-to-first-byte of the requests executed by the
 * HttpClient.
 * <p>
 * The policy is global: all sampled requests feed the same bounded window. A request rule can
 * enable or disable the policy for the requests it matches, but it cannot change the policy
 * parameters.
 */
public class MicrometerAdaptiveTimeoutPolicy implements AdaptiveTimeoutPolicy {

	private final HttpClient5Config config;
	private final Clock clock;
	private final LatencyWindow window;
	private final AtomicLong totalSamples = new AtomicLong();
	private final AtomicLong timeoutCount = new AtomicLong();
	private final AtomicReference<State> state = new AtomicReference<>(State.NOT_READY);
	private final Object refreshLock = new Object();

	public MicrometerAdaptiveTimeoutPolicy(HttpClient5Config config, Clock clock) {
		if (config == null) {
			throw new IllegalArgumentException("HttpClient5Config must not be null");
		}
		if (clock == null) {
			throw new IllegalArgumentException("Clock must not be null");
		}
		this.config = config;
		this.clock = clock;
		AdaptiveTimeoutProperties properties = config.getAdaptiveTimeout();
		validate(properties);
		this.window = new LatencyWindow(properties.getMaxSamples());
	}

	@Override
	public Timeout resolveTimeout(String method, String uri, RequestConfigProperties properties) {
		AdaptiveTimeoutProperties global = config.getAdaptiveTimeout();
		if (!isEnabled(global, properties)) {
			return null;
		}
		long now = clock.monotonicTime();
		State current = state.get();
		if (current.isReady() && now < current.getNextRefreshNanos()) {
			return current.getTimeout();
		}
		synchronized (refreshLock) {
			current = state.get();
			if (current.isReady() && now < current.getNextRefreshNanos()) {
				return current.getTimeout();
			}
			return refresh(global, now, current);
		}
	}

	private Timeout refresh(AdaptiveTimeoutProperties properties, long now, State current) {
		if (totalSamples.get() < properties.getWarmupRequests()) {
			return current.isReady() ? current.getTimeout() : null;
		}
		long refreshNanos = properties.getRefreshInterval().toNanoseconds();
		LatencyWindow.Snapshot snapshot = window.snapshot(now, properties.getWindow().toNanoseconds(),
				properties.getMinSamples(), properties.getPercentile());
		if (snapshot == null) {
			if (current.isReady()) {
				State kept = current.stale(now, refreshNanos);
				state.set(kept);
				return kept.getTimeout();
			}
			return null;
		}

		long lowerBound = (long) Math.ceil(snapshot.getMinNanos() * properties.getLowerBoundFactor());
		long floor = Math.max(lowerBound, properties.getMinTimeout().toNanoseconds());
		long computed = (long) Math.ceil(snapshot.getPercentileNanos() * properties.getFactor());
		long capped = Math.min(Math.max(floor, computed), properties.getMaxTimeout().toNanoseconds());
		Timeout timeout = Timeout.ofNanoseconds(capped);
		state.set(new State(timeout, snapshot.getPercentileNanos(), snapshot.getMinNanos(), snapshot.getSamples(),
				now + refreshNanos, clock.wallTime(), false));
		return timeout;
	}

	private boolean isEnabled(AdaptiveTimeoutProperties global, RequestConfigProperties properties) {
		AdaptiveTimeoutRuleProperties rule = properties != null ? properties.getAdaptiveTimeout() : null;
		if (rule != null && rule.getEnabled() != null) {
			return rule.getEnabled();
		}
		return global.isEnabled();
	}

	/**
	 * Records a successful time-to-first-byte sample.
	 */
	public void record(long durationNanos) {
		window.record(durationNanos, clock.monotonicTime());
		totalSamples.incrementAndGet();
	}

	/**
	 * Records a response timeout. When {@code include-timeouts} is enabled, the effective timeout
	 * is stored as a censored sample so that the percentile can grow instead of collapsing.
	 */
	public void recordTimeout(Timeout effectiveTimeout) {
		timeoutCount.incrementAndGet();
		AdaptiveTimeoutProperties properties = config.getAdaptiveTimeout();
		if (properties.isIncludeTimeouts() && effectiveTimeout != null) {
			record(Math.max(0L, effectiveTimeout.toNanoseconds()));
		}
	}

	/**
	 * Current effective timeout in seconds, or {@link Double#NaN} when the policy is not ready.
	 */
	public double getCurrentTimeoutSeconds() {
		State current = state.get();
		return current.isReady() ? current.getTimeout().toNanoseconds() / 1_000_000_000d : Double.NaN;
	}

	/**
	 * Current percentile value in seconds, or {@link Double#NaN} when the policy is not ready.
	 */
	public double getCurrentPercentileSeconds() {
		State current = state.get();
		return current.isReady() ? current.getPercentileNanos() / 1_000_000_000d : Double.NaN;
	}

	public boolean isReady() {
		return state.get().isReady();
	}

	public boolean isStale() {
		return state.get().isStale();
	}

	/**
	 * Current effective timeout in milliseconds, or {@code -1} when the policy is not ready.
	 */
	public long getCurrentTimeoutMillis() {
		State current = state.get();
		return current.isReady() ? current.getTimeout().toMilliseconds() : -1L;
	}

	/**
	 * Current percentile value in milliseconds, or {@code -1} when the policy is not ready.
	 */
	public long getCurrentPercentileMillis() {
		State current = state.get();
		return current.isReady() ? TimeUnit.NANOSECONDS.toMillis(current.getPercentileNanos()) : -1L;
	}

	/**
	 * Minimum observed TTFB in milliseconds, or {@code -1} when the policy is not ready.
	 */
	public long getCurrentMinObservedMillis() {
		State current = state.get();
		return current.isReady() ? TimeUnit.NANOSECONDS.toMillis(current.getMinObservedNanos()) : -1L;
	}

	/**
	 * Wall-clock time of the last successful computation, or {@code 0} when never computed.
	 */
	public long getLastComputedAtMillis() {
		return state.get().getLastComputedWallTimeMillis();
	}

	/**
	 * Age of the last successful computation in milliseconds, or {@code -1} when never computed.
	 */
	public long getLastComputedAgeMillis() {
		State current = state.get();
		return current.isReady() ? Math.max(0L, clock.wallTime() - current.getLastComputedWallTimeMillis()) : -1L;
	}

	/**
	 * Number of samples used by the last computation.
	 */
	public int getCurrentSamples() {
		State current = state.get();
		return current.isReady() ? current.getSamples() : 0;
	}

	/**
	 * Number of samples currently held in the ring buffer, including expired ones.
	 */
	public int getWindowSamples() {
		return window.size();
	}

	public long getTotalSamples() {
		return totalSamples.get();
	}

	public long getTimeoutCount() {
		return timeoutCount.get();
	}

	private static void validate(AdaptiveTimeoutProperties p) {
		if (p == null) {
			throw new IllegalArgumentException("spring.httpclient5.adaptive-timeout configuration is missing");
		}
		if (p.getPercentile() <= 0d || p.getPercentile() >= 1d) {
			throw new IllegalArgumentException("spring.httpclient5.adaptive-timeout.percentile must be in ]0,1[");
		}
		if (p.getFactor() < 1d) {
			throw new IllegalArgumentException("spring.httpclient5.adaptive-timeout.factor must be >= 1");
		}
		if (p.getLowerBoundFactor() < 1d) {
			throw new IllegalArgumentException("spring.httpclient5.adaptive-timeout.lower-bound-factor must be >= 1");
		}
		if (p.getWindow() == null || p.getWindow().toNanoseconds() <= 0) {
			throw new IllegalArgumentException("spring.httpclient5.adaptive-timeout.window must be positive");
		}
		if (p.getRefreshInterval() == null || p.getRefreshInterval().toNanoseconds() < 0) {
			throw new IllegalArgumentException("spring.httpclient5.adaptive-timeout.refresh-interval must be >= 0");
		}
		if (p.getMaxSamples() <= 0 || p.getMaxSamples() > 100_000) {
			throw new IllegalArgumentException("spring.httpclient5.adaptive-timeout.max-samples must be in ]0,100000]");
		}
		if (p.getMinSamples() <= 0) {
			throw new IllegalArgumentException("spring.httpclient5.adaptive-timeout.min-samples must be > 0");
		}
		if (p.getMinSamples() > p.getMaxSamples()) {
			throw new IllegalArgumentException("spring.httpclient5.adaptive-timeout.min-samples must be <= max-samples");
		}
		if (p.getWarmupRequests() < 0) {
			throw new IllegalArgumentException("spring.httpclient5.adaptive-timeout.warmup-requests must be >= 0");
		}
		if (p.getMinTimeout() == null || p.getMaxTimeout() == null) {
			throw new IllegalArgumentException("spring.httpclient5.adaptive-timeout min/max timeouts must be set");
		}
		if (p.getMinTimeout().toNanoseconds() < 0) {
			throw new IllegalArgumentException("spring.httpclient5.adaptive-timeout.min-timeout must be >= 0");
		}
		if (p.getMaxTimeout().toNanoseconds() <= 0) {
			throw new IllegalArgumentException("spring.httpclient5.adaptive-timeout.max-timeout must be > 0");
		}
		if (p.getMinTimeout().toNanoseconds() > p.getMaxTimeout().toNanoseconds()) {
			throw new IllegalArgumentException("spring.httpclient5.adaptive-timeout.min-timeout must be <= max-timeout");
		}
		AdaptiveTimeoutProperties.DiagnosticsProperties diagnostics = p.getDiagnostics();
		if (diagnostics == null) {
			throw new IllegalArgumentException("spring.httpclient5.adaptive-timeout.diagnostics configuration is missing");
		}
		if (diagnostics.isLogEnabled() && (diagnostics.getLogInterval() == null
				|| diagnostics.getLogInterval().toNanoseconds() <= 0)) {
			throw new IllegalArgumentException(
					"spring.httpclient5.adaptive-timeout.diagnostics.log-interval must be positive when log-enabled is true");
		}
		if (diagnostics.getLogMaxRules() <= 0) {
			throw new IllegalArgumentException(
					"spring.httpclient5.adaptive-timeout.diagnostics.log-max-rules must be > 0");
		}
	}

	private static final class State {

		private static final State NOT_READY = new State(null, 0L, 0L, 0, 0L, 0L, false);

		private final Timeout timeout;
		private final long percentileNanos;
		private final long minObservedNanos;
		private final int samples;
		private final long nextRefreshNanos;
		private final long lastComputedWallTimeMillis;
		private final boolean stale;

		private State(Timeout timeout, long percentileNanos, long minObservedNanos, int samples, long nextRefreshNanos,
				long lastComputedWallTimeMillis, boolean stale) {
			this.timeout = timeout;
			this.percentileNanos = percentileNanos;
			this.minObservedNanos = minObservedNanos;
			this.samples = samples;
			this.nextRefreshNanos = nextRefreshNanos;
			this.lastComputedWallTimeMillis = lastComputedWallTimeMillis;
			this.stale = stale;
		}

		private boolean isReady() {
			return timeout != null;
		}

		private Timeout getTimeout() {
			return timeout;
		}

		private long getPercentileNanos() {
			return percentileNanos;
		}

		private long getMinObservedNanos() {
			return minObservedNanos;
		}

		private int getSamples() {
			return samples;
		}

		private long getNextRefreshNanos() {
			return nextRefreshNanos;
		}

		private long getLastComputedWallTimeMillis() {
			return lastComputedWallTimeMillis;
		}

		private boolean isStale() {
			return stale;
		}

		private State stale(long now, long intervalNanos) {
			return new State(timeout, percentileNanos, minObservedNanos, samples, now + intervalNanos,
					lastComputedWallTimeMillis, true);
		}
	}
}
