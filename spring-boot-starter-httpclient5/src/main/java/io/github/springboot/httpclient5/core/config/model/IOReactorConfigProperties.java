package io.github.springboot.httpclient5.core.config.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.hc.core5.reactor.IOReactorConfig;

import io.github.springboot.httpclient5.core.config.ConfigProvider;
import lombok.experimental.Delegate;

public class IOReactorConfigProperties implements ConfigProvider<IOReactorConfig> {
	@Delegate
	private IOReactorConfig.Builder builder = IOReactorConfig.custom();
	
	@Delegate
	public IOReactorConfig get() {
		return builder.build();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this.get(), ToStringStyle.JSON_STYLE);
	}

}
