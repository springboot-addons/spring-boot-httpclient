package io.github.springboot.httpclient5.actuator.adaptive;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.async.AsyncExecCallback;
import org.apache.hc.client5.http.async.AsyncExecChain;
import org.apache.hc.client5.http.async.AsyncExecChainHandler;
import org.apache.hc.client5.http.classic.ExecChain;
import org.apache.hc.client5.http.classic.ExecChainHandler;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncDataConsumer;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.apache.hc.core5.util.Timeout;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import io.github.springboot.httpclient5.core.interceptors.RequestConfigExecChainHandler;
import io.micrometer.core.instrument.Clock;

/**
 * Records the time-to-first-byte of the requests and feeds the adaptive timeout policy.
 * <p>
 * The handler is placed just inside the observation handler and inside the Resilience4j handler
 * when that starter is present, so the sample reflects one client execution attempt. Redirects and
 * retries handled by HttpClient below this handler are included in the measured duration.
 */
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class AdaptiveTimeoutExecChainHandler implements ExecChainHandler, AsyncExecChainHandler {

	private final MicrometerAdaptiveTimeoutPolicy policy;
	private final AdaptiveTimeoutRuleTracker tracker;
	private final Clock clock;

	public AdaptiveTimeoutExecChainHandler(MicrometerAdaptiveTimeoutPolicy policy,
			AdaptiveTimeoutRuleTracker tracker, Clock clock) {
		this.policy = policy;
		this.tracker = tracker;
		this.clock = clock;
	}

	@Override
	public ClassicHttpResponse execute(ClassicHttpRequest request, ExecChain.Scope scope, ExecChain chain)
			throws IOException, HttpException {
		long start = clock.monotonicTime();
		try {
			ClassicHttpResponse response = chain.proceed(request, scope);
			policy.record(clock.monotonicTime() - start);
			tracker.record(rule(scope.clientContext), effectiveTimeout(scope.clientContext), false, false);
			return response;
		} catch (IOException | HttpException | RuntimeException e) {
			recordFailure(e, scope.clientContext);
			throw e;
		}
	}

	@Override
	public void execute(HttpRequest request, AsyncEntityProducer entityProducer, AsyncExecChain.Scope scope,
			AsyncExecChain chain, AsyncExecCallback asyncExecCallback) throws HttpException, IOException {
		long start = clock.monotonicTime();
		try {
			chain.proceed(request, entityProducer, scope, new AsyncExecCallback() {

				private final AtomicBoolean recorded = new AtomicBoolean();

				@Override
				public AsyncDataConsumer handleResponse(HttpResponse response, EntityDetails entityDetails)
						throws HttpException, IOException {
					AsyncDataConsumer consumer = asyncExecCallback.handleResponse(response, entityDetails);
					record(false);
					return consumer;
				}

				@Override
				public void handleInformationResponse(HttpResponse response) throws HttpException, IOException {
					asyncExecCallback.handleInformationResponse(response);
				}

				@Override
				public void completed() {
					asyncExecCallback.completed();
					record(false);
				}

				@Override
				public void failed(Exception ex) {
					asyncExecCallback.failed(ex);
					record(true, ex);
				}

				private void record(boolean failure) {
					record(failure, null);
				}

				private void record(boolean failure, Throwable error) {
					if (!recorded.compareAndSet(false, true)) {
						return;
					}
					if (!failure) {
						policy.record(clock.monotonicTime() - start);
						tracker.record(rule(scope.clientContext), effectiveTimeout(scope.clientContext), false,
								false);
					} else if (error != null) {
						recordFailure(error, scope.clientContext);
					}
				}
			});
		} catch (IOException | HttpException | RuntimeException e) {
			recordFailure(e, scope.clientContext);
			throw e;
		}
	}

	private void recordFailure(Throwable error, HttpClientContext context) {
		boolean responseTimeout = isResponseTimeout(error);
		Timeout timeout = effectiveTimeout(context);
		if (responseTimeout) {
			policy.recordTimeout(timeout);
		}
		String rule = rule(context);
		if (rule != null) {
			tracker.record(rule, timeout, responseTimeout, !responseTimeout);
		}
	}

	private static String rule(HttpClientContext context) {
		if (context == null) {
			return null;
		}
		Object value = context.getAttribute(RequestConfigExecChainHandler.REQUEST_CONFIG_RULE);
		return value instanceof String rule ? rule : null;
	}

	private static boolean isResponseTimeout(Throwable error) {
		Throwable current = error;
		while (current != null) {
			if (current instanceof ConnectTimeoutException) {
				// Connect timeouts are not governed by response-timeout.
				return false;
			}
			if (current instanceof SocketTimeoutException) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private static Timeout effectiveTimeout(HttpClientContext context) {
		if (context == null) {
			return null;
		}
		RequestConfig requestConfig = context.getRequestConfig();
		return requestConfig != null ? requestConfig.getResponseTimeout() : null;
	}
}
