package io.github.springboot.httpclient5.resilience4j;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.hc.client5.http.async.AsyncExecCallback;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncDataConsumer;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry.Context;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Resilience4JAsyncExecCallback implements AsyncExecCallback {
    private final AsyncExecCallback delegate;
	private CircuitBreaker circuitBreaker;
	long start = System.nanoTime();

    public Resilience4JAsyncExecCallback(CircuitBreaker circuitBreaker,
                    AsyncExecCallback delegate,
                    HttpRequest request) {
        this.circuitBreaker = circuitBreaker;
		this.delegate = delegate;
    }

    @Override
    public AsyncDataConsumer handleResponse(HttpResponse response, EntityDetails entityDetails) throws HttpException, IOException {
		internalHandleResponse(response);
        return delegate.handleResponse(response, entityDetails);
    }

    @Override
    public void handleInformationResponse(HttpResponse response) throws HttpException, IOException {
		internalHandleResponse(response);
        delegate.handleInformationResponse(response);
    }

	private void internalHandleResponse(HttpResponse response) {
		final int statusCode = response.getCode();

		final long durationInNanos = System.nanoTime() - start;
		if (isError(statusCode)) {
			circuitBreaker.onError(durationInNanos, TimeUnit.NANOSECONDS,
					new IOException("Http Status Error " + statusCode));
			log.debug("After http 5xx circuit breakers state {}, metrics {}", circuitBreaker.getState(), ToStringBuilder.reflectionToString(circuitBreaker.getMetrics())) ;
		} else {
			circuitBreaker.onSuccess(durationInNanos, TimeUnit.NANOSECONDS);
		}
	}

    @Override
    public void completed() {
        delegate.completed();
    }

    @Override
    public void failed(Exception cause) {
		final long durationInNanos = System.nanoTime() - start;
		circuitBreaker.onError(durationInNanos, TimeUnit.NANOSECONDS, cause);
		log.debug("After exception circuit breakers state {}, metrics {}", circuitBreaker.getState(), ToStringBuilder.reflectionToString(circuitBreaker.getMetrics())) ;
        delegate.failed(cause);
    }

	private boolean isError(int code ) {
		return code >= 500;
	}
}