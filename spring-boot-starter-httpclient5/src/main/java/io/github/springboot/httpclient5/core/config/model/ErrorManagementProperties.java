package io.github.springboot.httpclient5.core.config.model;

import lombok.Data;

@Data
public class ErrorManagementProperties {
	private String circuitName = "default";
	private Integer maxAttempts = 1 ;
	private Integer waitDuration = 0 ;
	private String brokenCircuitAction = "error";
}

