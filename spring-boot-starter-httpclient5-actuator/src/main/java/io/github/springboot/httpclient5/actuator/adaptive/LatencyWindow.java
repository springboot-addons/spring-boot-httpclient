package io.github.springboot.httpclient5.actuator.adaptive;

import java.util.Arrays;

/**
 * Bounded ring buffer of time-to-first-byte durations, filtered by a sliding time window.
 * <p>
 * The buffer keeps at most {@code capacity} samples. Percentiles are computed on demand from a
 * copy of the valid samples, so the cost is paid at the refresh rate, not on every request.
 */
final class LatencyWindow {

	private final long[] durations;
	private final long[] timestamps;
	private final long[] scratch;
	private int count;
	private int next;

	LatencyWindow(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be positive");
		}
		this.durations = new long[capacity];
		this.timestamps = new long[capacity];
		this.scratch = new long[capacity];
	}

	synchronized void record(long durationNanos, long nowNanos) {
		durations[next] = Math.max(0L, durationNanos);
		timestamps[next] = nowNanos;
		next = (next + 1) % durations.length;
		if (count < durations.length) {
			count++;
		}
	}

	synchronized Snapshot snapshot(long nowNanos, long windowNanos, int minSamples, double percentile) {
		if (count < minSamples) {
			return null;
		}
		int valid = 0;
		for (int i = 0; i < count; i++) {
			long age = nowNanos - timestamps[i];
			if (age >= 0 && age <= windowNanos) {
				scratch[valid++] = durations[i];
			}
		}
		if (valid < minSamples) {
			return null;
		}
		Arrays.sort(scratch, 0, valid);
		long min = scratch[0];
		return new Snapshot(min, percentile(scratch, valid, 0.50d), percentile(scratch, valid, 0.90d),
				percentile(scratch, valid, 0.99d), percentile(scratch, valid, percentile), valid);
	}

	private static long percentile(long[] values, int count, double percentile) {
		if (count == 0) {
			return 0L;
		}
		int index = (int) Math.ceil(percentile * count) - 1;
		if (index < 0) {
			index = 0;
		} else if (index >= count) {
			index = count - 1;
		}
		return values[index];
	}

	synchronized int size() {
		return count;
	}

	static final class Snapshot {

		private final long minNanos;
		private final long p50Nanos;
		private final long p90Nanos;
		private final long p99Nanos;
		private final long percentileNanos;
		private final int samples;

		Snapshot(long minNanos, long p50Nanos, long p90Nanos, long p99Nanos, long percentileNanos,
				int samples) {
			this.minNanos = minNanos;
			this.p50Nanos = p50Nanos;
			this.p90Nanos = p90Nanos;
			this.p99Nanos = p99Nanos;
			this.percentileNanos = percentileNanos;
			this.samples = samples;
		}

		long getMinNanos() {
			return minNanos;
		}

		long getP50Nanos() {
			return p50Nanos;
		}

		long getP90Nanos() {
			return p90Nanos;
		}

		long getP99Nanos() {
			return p99Nanos;
		}

		long getPercentileNanos() {
			return percentileNanos;
		}

		int getSamples() {
			return samples;
		}
	}
}
