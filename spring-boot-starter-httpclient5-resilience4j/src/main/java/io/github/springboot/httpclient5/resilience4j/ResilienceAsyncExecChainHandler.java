package io.github.springboot.httpclient5.resilience4j;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.async.AsyncExecCallback;
import org.apache.hc.client5.http.async.AsyncExecChain;
import org.apache.hc.client5.http.async.AsyncExecChainHandler;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.impl.BasicEntityDetails;
import org.apache.hc.core5.http.nio.AsyncDataConsumer;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * ResilienceExecChainHandler
 */
@Slf4j
public class ResilienceAsyncExecChainHandler implements AsyncExecChainHandler {

	private final class DefinedResponseAsyncExecCallback implements AsyncExecCallback {
		private final SimpleHttpResponse definedErrorResponse;
		private final AsyncExecCallback asyncExecCallback;

		private DefinedResponseAsyncExecCallback(SimpleHttpResponse definedErrorResponse,
				AsyncExecCallback asyncExecCallback) {
			this.definedErrorResponse = definedErrorResponse;
			this.asyncExecCallback = asyncExecCallback;
		}

		@Override
		public AsyncDataConsumer handleResponse(HttpResponse response, EntityDetails entityDetails)
				throws HttpException, IOException {
			return asyncExecCallback.handleResponse(definedErrorResponse, entityDetails);
		}

		@Override
		public void handleInformationResponse(HttpResponse response) throws HttpException, IOException {
			asyncExecCallback.handleInformationResponse(definedErrorResponse);
		}

		@Override
		public void failed(Exception cause) {
			asyncExecCallback.failed(cause);
			//asyncExecCallback.completed();
		}

		@Override
		public void completed() {
			asyncExecCallback.completed();
		}
	}

	private final CircuitBreakerRegistry cbRegistry;
	private final RateLimiterRegistry rlregstry;
	private final HttpClient5Config config;

	public ResilienceAsyncExecChainHandler(HttpClient5Config config, CircuitBreakerRegistry cbRegistry,
			RateLimiterRegistry rlregstry) {
		this.config = config;
		this.cbRegistry = cbRegistry;
		this.rlregstry = rlregstry;
	}

	@Override
	public void execute(HttpRequest request, AsyncEntityProducer entityProducer,
			org.apache.hc.client5.http.async.AsyncExecChain.Scope scope, AsyncExecChain chain,
			AsyncExecCallback asyncExecCallback) throws HttpException, IOException {

		String method = request.getMethod();
		String requestUri;
		try {
			requestUri = request.getUri().toString();
		} catch (URISyntaxException e) {
			throw new IOException(e) ;
		}
		RequestConfigProperties requestConfigProperties = config.getRequestConfigProperties(method, requestUri);
		String circuitName = requestConfigProperties.getErrorManagement().getCircuitName() ;

		final CircuitBreaker circuitBreaker = cbRegistry.circuitBreaker(circuitName);
		if (log.isTraceEnabled()) {
			log.trace("Before circuit breakers {} state {}, metrics {}", circuitBreaker.getName(), circuitBreaker.getState(), ToStringBuilder.reflectionToString(circuitBreaker.getMetrics())) ;
		}

		if (circuitBreaker.tryAcquirePermission()) {
			if (circuitName != HttpClientResilience4jAutoConfiguration.DEFAULT_CIRCUIT) {
				final RateLimiter rateLimiter = rlregstry.rateLimiter(circuitName);
				RateLimiter.waitForPermission(rateLimiter);
			}

			final Resilience4JAsyncExecCallback instrumentedAsyncExecCallback =
	        		new Resilience4JAsyncExecCallback(circuitBreaker, asyncExecCallback, request);
			
			chain.proceed(request, entityProducer, scope, instrumentedAsyncExecCallback);
		} else {
			String action = requestConfigProperties.getErrorManagement().getBrokenCircuitAction() ;
			if (StringUtils.isNumeric(action)) {
				SimpleHttpResponse definedErrorResponse = new SimpleHttpResponse(Integer.parseInt(action),
						"Broken circuit : " + circuitBreaker.toString()) ;
				asyncExecCallback.handleInformationResponse(definedErrorResponse);
				asyncExecCallback.handleResponse(definedErrorResponse, null) ;
				asyncExecCallback.completed();
				//chain.proceed(request, entityProducer, scope, new DefinedResponseAsyncExecCallback(definedErrorResponse, asyncExecCallback));
			} else {
				throw new ClientProtocolException("Broken circuit : " + circuitBreaker.toString() + " is closed");
			}
		}
	}
	
}
