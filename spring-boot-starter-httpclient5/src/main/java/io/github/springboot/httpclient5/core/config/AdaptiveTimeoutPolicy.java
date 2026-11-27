package io.github.springboot.httpclient5.core.config;

import org.apache.hc.core5.util.Timeout;

import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;

/**
 * Optional SPI used to compute an adaptive response timeout for a request.
 * <p>
 * Implementations are looked up lazily by {@link HttpClient5Config}. When no bean is available,
 * or when the policy returns {@code null}, the statically configured {@code response-timeout} is
 * kept.
 * <p>
 * The policy is applied after the per-request configuration has been resolved and after the
 * {@link RequestConfigCustomizer} has run, so it can replace the static timeout for every caller
 * (exec chain handler, request interceptor, resilience handler).
 */
public interface AdaptiveTimeoutPolicy {

	/**
	 * Resolves the response timeout to apply to the given request.
	 *
	 * @param method the HTTP method
	 * @param uri the absolute request URI
	 * @param properties the resolved request configuration, including the per-rule
	 *        {@code adaptive-timeout} override
	 * @return the timeout to apply, or {@code null} to keep the static value
	 */
	Timeout resolveTimeout(String method, String uri, RequestConfigProperties properties);
}
