package io.github.springboot.httpclient5.core.config.model;

import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Global configuration of the adaptive response timeout policy.
 * <p>
 * The policy replaces the static {@code response-timeout} of a request with a value derived from
 * the observed time-to-first-byte (TTFB):
 * <ul>
 * <li>the configured percentile of the samples in the sliding window;</li>
 * <li>multiplied by {@link #factor};</li>
 * <li>floored by {@code max(minTimeout, lowerBoundFactor * min observed TTFB)};</li>
 * <li>capped by {@link #maxTimeout}.</li>
 * </ul>
 * The policy stays inactive until {@link #warmupRequests} samples have been recorded and at least
 * {@link #minSamples} samples are present in the current window.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AdaptiveTimeoutProperties {

	public static final double DEFAULT_PERCENTILE = 0.99d;
	public static final double DEFAULT_FACTOR = 1.5d;
	public static final double DEFAULT_LOWER_BOUND_FACTOR = 2.0d;
	public static final int DEFAULT_MAX_SAMPLES = 1000;
	public static final int DEFAULT_MIN_SAMPLES = 20;
	public static final int DEFAULT_WARMUP_REQUESTS = 100;
	public static final TimeValue DEFAULT_WINDOW = TimeValue.ofMinutes(5);
	public static final TimeValue DEFAULT_REFRESH_INTERVAL = TimeValue.ofSeconds(10);
	public static final Timeout DEFAULT_MIN_TIMEOUT = Timeout.ZERO_MILLISECONDS;
	public static final Timeout DEFAULT_MAX_TIMEOUT = Timeout.ofSeconds(60);

	/**
	 * Enable the policy by default. A rule can override this value through
	 * {@code spring.httpclient5.request-config.<rule>.adaptive-timeout.enabled}.
	 */
	@Builder.Default
	private boolean enabled = false;

	/**
	 * Percentile of the TTFB distribution used to compute the timeout (between 0 and 1).
	 */
	@Builder.Default
	private double percentile = DEFAULT_PERCENTILE;

	/**
	 * Safety multiplier applied to the percentile.
	 */
	@Builder.Default
	private double factor = DEFAULT_FACTOR;

	/**
	 * Multiplier applied to the minimum observed TTFB to compute the lower bound.
	 */
	@Builder.Default
	private double lowerBoundFactor = DEFAULT_LOWER_BOUND_FACTOR;

	/**
	 * Sliding time window used to select samples.
	 */
	@Builder.Default
	private TimeValue window = DEFAULT_WINDOW;

	/**
	 * Minimum delay between two recalculations. Zero recalculates on every resolution.
	 */
	@Builder.Default
	private TimeValue refreshInterval = DEFAULT_REFRESH_INTERVAL;

	/**
	 * Maximum number of samples kept in memory.
	 */
	@Builder.Default
	private int maxSamples = DEFAULT_MAX_SAMPLES;

	/**
	 * Minimum number of samples required in the window to compute a value.
	 */
	@Builder.Default
	private int minSamples = DEFAULT_MIN_SAMPLES;

	/**
	 * Number of samples recorded before the policy starts replacing the static timeout.
	 */
	@Builder.Default
	private int warmupRequests = DEFAULT_WARMUP_REQUESTS;

	/**
	 * Optional absolute lower bound. The effective lower bound is the greater of this value and
	 * {@code lowerBoundFactor * min observed TTFB}.
	 */
	@Builder.Default
	private Timeout minTimeout = DEFAULT_MIN_TIMEOUT;

	/**
	 * Upper bound applied to the computed timeout.
	 */
	@Builder.Default
	private Timeout maxTimeout = DEFAULT_MAX_TIMEOUT;

	/**
	 * Record response timeouts as censored samples at the effective timeout. This prevents the
	 * percentile from collapsing when the policy truncates the slowest requests.
	 */
	@Builder.Default
	private boolean includeTimeouts = true;
}
