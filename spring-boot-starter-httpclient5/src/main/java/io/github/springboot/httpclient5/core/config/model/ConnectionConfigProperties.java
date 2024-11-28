package io.github.springboot.httpclient5.core.config.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.hc.client5.http.config.ConnectionConfig;

import io.github.springboot.httpclient5.core.config.ConfigProvider;
import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

public class ConnectionConfigProperties implements ConfigProvider<ConnectionConfig> {
	@Delegate
	private ConnectionConfig.Builder builder = ConnectionConfig.custom()
					.setConnectTimeout(HttpClient5Config.DEFAULT_CONNECT_TIMEOUT);
	
	@Setter
	@Getter
	private Integer maxConnections ;

	
	@Delegate
	public ConnectionConfig get() {
		return builder.build();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this.get(), ToStringStyle.JSON_STYLE);
	}

}
