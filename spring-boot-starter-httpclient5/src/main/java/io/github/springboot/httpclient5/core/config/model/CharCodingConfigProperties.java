package io.github.springboot.httpclient5.core.config.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.hc.core5.http.config.CharCodingConfig;

import io.github.springboot.httpclient5.core.config.ConfigProvider;
import lombok.experimental.Delegate;

public class CharCodingConfigProperties implements ConfigProvider<CharCodingConfig> {
	@Delegate
	private CharCodingConfig.Builder builder = CharCodingConfig.custom();
	
	@Delegate
	public CharCodingConfig get() {
		return builder.build();
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this.get(), ToStringStyle.JSON_STYLE);
	}
}
