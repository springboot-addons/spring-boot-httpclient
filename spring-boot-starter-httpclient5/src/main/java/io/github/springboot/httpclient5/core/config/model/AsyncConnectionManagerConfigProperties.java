package io.github.springboot.httpclient5.core.config.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

public class AsyncConnectionManagerConfigProperties implements DefaultConfigConfigurer {
	private static final int DEFAULT_CLOSE_IDLE_CONNECTION_WAIT_TIME_SECS = 30;

	@Delegate
	private PoolingAsyncClientConnectionManagerBuilder builder = PoolingAsyncClientConnectionManagerBuilder.create();
	
	@Setter
	@Getter
	@NestedConfigurationProperty
	private ConnectionConfigProperties defaultConnectionConfig ;
	
	@Setter
	@Getter
	@NestedConfigurationProperty
	private Map<String, ConnectionConfigProperties> hostConfig = new HashMap<>();
	
	@Setter
	@Getter
	private TimeValue connectionIdleTimeout = TimeValue.ofSeconds(DEFAULT_CLOSE_IDLE_CONNECTION_WAIT_TIME_SECS) ;
	
	public PoolingAsyncClientConnectionManagerBuilder get() {
		return builder;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this.get(), ToStringStyle.JSON_STYLE);
	}

}
