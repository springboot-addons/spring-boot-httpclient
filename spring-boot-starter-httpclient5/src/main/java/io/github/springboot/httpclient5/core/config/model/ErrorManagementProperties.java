package io.github.springboot.httpclient5.core.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ErrorManagementProperties {
	@Builder.Default
	private String circuitName = "default";
	@Builder.Default
	private Integer maxAttempts = 1 ;
	@Builder.Default
	private Integer waitDuration = 0 ;
	@Builder.Default
	private String brokenCircuitAction = "error";
}

