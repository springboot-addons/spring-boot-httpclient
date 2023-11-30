package io.github.springboot.httpclient5.core.config.model;

import org.apache.hc.core5.util.TimeValue;

import lombok.Data;

@Data
public class RetryConfig {
	private Integer maxRetries;
	private TimeValue retryInterval ;
}
