package io.github.springboot.httpclient5.actuator;

import java.io.IOException;

import org.apache.hc.client5.http.classic.ExecChain;
import org.apache.hc.client5.http.classic.ExecChain.Scope;
import org.apache.hc.client5.http.classic.ExecChainHandler;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.httpclient5.HttpClientMetricNameStrategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ActuatorMetricExecChainHandler implements ExecChainHandler {
	private final MetricRegistry registry;
	private final HttpClientMetricNameStrategy metricNameStrategy;
	private final String name;

	public ActuatorMetricExecChainHandler(MetricRegistry registry,
			HttpClientMetricNameStrategy metricNameStrategy) {
		this(registry, metricNameStrategy, null);
	}

	public ActuatorMetricExecChainHandler(MetricRegistry registry,
			HttpClientMetricNameStrategy metricNameStrategy, String name) {
		this.registry = registry;
		this.name = name;
		this.metricNameStrategy = metricNameStrategy;
	}

	@Override
	public ClassicHttpResponse execute(ClassicHttpRequest request, Scope scope, ExecChain chain)
			throws IOException, HttpException {

		final Timer.Context timerContext = timer(request).time();
		try {
			log.debug("before ActuatorMetricExecChainHandler.doExecute");
			return chain.proceed(request, scope);
		} catch (HttpException | IOException e) {
			meter(e).mark();
			throw e;
		} finally {
			log.debug("after ActuatorMetricExecChainHandler.doExecute");
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
