package io.github.springboot.httpclient5.core.config.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

public class ConnectionManagerConfigProperties {
	@Delegate
	private PoolingHttpClientConnectionManagerBuilder builder = PoolingHttpClientConnectionManagerBuilder.create();
	
	@Setter
	@Getter
	@NestedConfigurationProperty
	private SocketConfigProperties socketConfig = new SocketConfigProperties();
	
	@Setter
	@Getter
	@NestedConfigurationProperty
	private Map<String, Integer> hostConfig = new HashMap<>();
	
	public PoolingHttpClientConnectionManagerBuilder get() {
		return builder;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this.get(), ToStringStyle.JSON_STYLE);
	}

}
