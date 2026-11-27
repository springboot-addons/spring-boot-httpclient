package io.github.springboot.httpclient5.actuator.adaptive;

import java.util.Map;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * Read-only Actuator endpoint exposing the adaptive timeout state and the per-rule comparison
 * between the configured and effective timeouts.
 * <p>
 * Endpoint id: {@code httpclient5-adaptive-timeout}. It must be exposed like any other endpoint:
 * {@code management.endpoints.web.exposure.include=httpclient5-adaptive-timeout}.
 */
@Endpoint(id = "httpclient5-adaptive-timeout")
public class AdaptiveTimeoutEndpoint {

	private final AdaptiveTimeoutDiagnostics diagnostics;

	public AdaptiveTimeoutEndpoint(AdaptiveTimeoutDiagnostics diagnostics) {
		this.diagnostics = diagnostics;
	}

	@ReadOperation
	public Map<String, Object> snapshot() {
		return diagnostics.snapshot();
	}
}
