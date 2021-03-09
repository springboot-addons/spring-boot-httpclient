package io.github.springboot.httpclient.actuator;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import io.github.springboot.httpclient.core.config.model.HttpClientConfiguration;

/**
 * Http Client endpoint
 *
 * @author sru
 */
@Endpoint(id = "httpclient")
public class HttpClientEndpoint {
	private final HttpClientConfiguration properties;
	private final MetricRegistry registry;

	public HttpClientEndpoint(HttpClientConfiguration configuration, MetricRegistry registry) {
		this.properties = configuration;
		this.registry = registry;
	}

	@ReadOperation
	public Map<String, Object> getData() {
		final Map<String, Object> info = new HashMap<>();
		info.put("author", "stephane.routhiau@gmail.com");
		info.put("httpcomponents", "http://hc.apache.org");
		info.put("metrics", getMetrics());
		info.put("config", properties);
		return info;
	}

	public Map<String, Object> getMetrics() {
		final Map<String, Object> metrics = new HashMap<>();
		// gauge
		final SortedMap<String, Gauge> gauges = registry
				.getGauges((name, metric) -> name.startsWith("org.apache.http.conn.HttpClientConnectionManager."));
		for (final Map.Entry<String, Gauge> entry : gauges.entrySet()) {
			metrics.put(entry.getKey(), entry.getValue().getValue());
		}
		// timer
		final SortedMap<String, com.codahale.metrics.Timer> timers = registry
				.getTimers((name, metric) -> name.startsWith("org.apache.http.client.HttpClient."));
		for (final Map.Entry<String, Timer> entry : timers.entrySet()) {
			metrics.putAll(convertTimerToMap(entry.getKey(), entry.getValue()));
		}
		return metrics;
	}

	public Map<String, Object> convertTimerToMap(String name, Timer timer) {
		final Map<String, Object> map = new HashMap<>();
		map.put(name + ".count", timer.getCount());
		map.put(name + ".oneMinuteRate", timer.getOneMinuteRate());
		map.put(name + ".fiveMinuteRate", timer.getFiveMinuteRate());
		map.put(name + ".fifteenMinuteRate", timer.getFifteenMinuteRate());
		map.put(name + ".meanRate", timer.getMeanRate());
		final Snapshot snapshot = timer.getSnapshot();
		map.put(name + ".snapshot.mean", snapshot.getMean());
		map.put(name + ".snapshot.max", snapshot.getMax());
		map.put(name + ".snapshot.min", snapshot.getMin());
		map.put(name + ".snapshot.median", snapshot.getMedian());
		map.put(name + ".snapshot.stdDev", snapshot.getStdDev());
		map.put(name + ".snapshot.75thPercentile", snapshot.get75thPercentile());
		map.put(name + ".snapshot.95thPercentile", snapshot.get95thPercentile());
		map.put(name + ".snapshot.98thPercentile", snapshot.get98thPercentile());
		map.put(name + ".snapshot.99thPercentile", snapshot.get99thPercentile());
		map.put(name + ".snapshot.999thPercentile", snapshot.get999thPercentile());
		return map;
	}
}
