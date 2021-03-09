package io.github.springboot.httpclient.actuator;

import java.io.IOException;

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.httpclient.HttpClientMetricNameStrategy;

import io.github.springboot.httpclient.core.internal.ChainableHttpRequestExecutor;
import io.github.springboot.httpclient.core.internal.HttpRequestExecutorChain;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChainableInstrumentedHttpRequestExecutor implements ChainableHttpRequestExecutor {
	private final MetricRegistry registry;
	private final HttpClientMetricNameStrategy metricNameStrategy;
	private final String name;

	public ChainableInstrumentedHttpRequestExecutor(MetricRegistry registry,
			HttpClientMetricNameStrategy metricNameStrategy) {
		this(registry, metricNameStrategy, null);
	}

	public ChainableInstrumentedHttpRequestExecutor(MetricRegistry registry,
			HttpClientMetricNameStrategy metricNameStrategy, String name) {
		this.registry = registry;
		this.name = name;
		this.metricNameStrategy = metricNameStrategy;
	}

	@Override
	public HttpResponse doExecute(HttpRequest request, HttpClientConnection conn, HttpContext context,
			HttpRequestExecutorChain chain) throws HttpException, IOException {

		final Timer.Context timerContext = timer(request).time();
		try {
			log.info("before ChainableInstrumentedHttpRequestExecutor.doExecute");
			return chain.doExecute(request, conn, context);
		} catch (HttpException | IOException e) {
			meter(e).mark();
			throw e;
		} finally {
			log.info("after ChainableInstrumentedHttpRequestExecutor.doExecute");
			timerContext.stop();
		}
	}

	private Timer timer(HttpRequest request) {
		return registry.timer(metricNameStrategy.getNameFor(name, request));
	}

	private Meter meter(Exception e) {
		return registry.meter(metricNameStrategy.getNameFor(name, e));
	}
}
