package io.github.springboot.httpclient5.core.config.model;

import org.apache.hc.core5.util.TimeValue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class RetryConfig {
	private Integer maxRetries;
	private TimeValue retryInterval ;
}
