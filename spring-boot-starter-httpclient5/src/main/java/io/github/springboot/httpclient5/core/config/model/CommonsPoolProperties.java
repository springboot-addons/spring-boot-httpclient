package io.github.springboot.httpclient5.core.config.model;

import java.util.Map;

import org.apache.hc.core5.util.TimeValue;

public interface CommonsPoolProperties {
	ConnectionConfigProperties getDefaultConnectionConfig();

	Map<String, ConnectionConfigProperties> getHostConfig();

	TimeValue getConnectionIdleTimeout();
}
