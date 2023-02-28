package io.github.springboot.httpclient.resilience4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.apache.http.config.Lookup;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.impl.conn.DefaultHttpClientConnectionOperator;
import org.apache.http.protocol.HttpContext;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.springboot.httpclient.core.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.core.utils.HttpClientUtils;

public class Resilience4JHttpClientConnectionOperator extends DefaultHttpClientConnectionOperator {

	private HttpClientConfigurationHelper config;
	private CircuitBreakerRegistry cbRegistry;

	public Resilience4JHttpClientConnectionOperator(HttpClientConfigurationHelper config,
			CircuitBreakerRegistry cbRegistry, Lookup<ConnectionSocketFactory> socketFactoryRegistry) {
		super(socketFactoryRegistry, null, null);
		this.config = config;
		this.cbRegistry = cbRegistry;
	}

	@Override
	public void connect(ManagedHttpClientConnection conn, HttpHost host, InetSocketAddress localAddress,
			int connectTimeout, SocketConfig socketConfig, HttpContext context) throws IOException {
		final String requestUri = HttpClientUtils.getUri(host, context).toString();
		final String circuitName = config.getConfigurationKeyForRequestUri(requestUri,
				HttpClientResilience4jAutoConfiguration.DEFAULT_CIRCUIT);

		final CircuitBreaker circuitBreaker = cbRegistry.circuitBreaker(circuitName);

		if (circuitBreaker.tryAcquirePermission()) {
			final long start = System.nanoTime();
			try {
				super.connect(conn, host, localAddress, connectTimeout, socketConfig, context);
				circuitBreaker.releasePermission();
//				final long durationInNanos = System.nanoTime() - start;
				//circuitBreaker.onSuccess(durationInNanos, TimeUnit.NANOSECONDS);
			} catch (final Throwable throwable) {
				final long durationInNanos = System.nanoTime() - start;
				circuitBreaker.onError(durationInNanos, TimeUnit.NANOSECONDS, throwable);
			}
		} else {
			throw new IOException("Broken circuit : " + circuitBreaker.toString());
		}
	}
}
