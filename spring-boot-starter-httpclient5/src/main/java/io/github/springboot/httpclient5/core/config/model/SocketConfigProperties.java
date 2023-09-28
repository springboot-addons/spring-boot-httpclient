package io.github.springboot.httpclient5.core.config.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.hc.core5.http.io.SocketConfig;

import io.github.springboot.httpclient5.core.config.ConfigProvider;
import lombok.experimental.Delegate;

public class SocketConfigProperties implements ConfigProvider<SocketConfig> {

	@Delegate
	private SocketConfig.Builder builder = SocketConfig.custom();
	
	@Delegate
	public SocketConfig get() {
		return builder.build();
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this.get(), ToStringStyle.JSON_STYLE);
	}
	
}
