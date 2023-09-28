package io.github.springboot.httpclient5.core.config.converters;

import org.apache.hc.core5.http.HttpHost;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;

@Component
@ConfigurationPropertiesBinding
public class HttpHostConverter implements Converter<String, HttpHost>{

	@Override
	@SneakyThrows
	public HttpHost convert(String source) {
		return HttpHost.create(source);
	}

}
