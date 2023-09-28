package io.github.springboot.httpclient5.core.config.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.hc.core5.http.config.Http1Config;

import io.github.springboot.httpclient5.core.config.ConfigProvider;
import lombok.experimental.Delegate;

public class Http1ConfigProperties implements ConfigProvider<Http1Config> {
	@Delegate
	private Http1Config.Builder builder = Http1Config.custom();
	
	@Delegate
	public Http1Config get() {
		return builder.build();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this.get(), ToStringStyle.JSON_STYLE);
	}

}
