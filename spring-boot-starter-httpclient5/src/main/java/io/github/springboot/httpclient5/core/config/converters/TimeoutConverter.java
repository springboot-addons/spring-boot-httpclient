package io.github.springboot.httpclient5.core.config.converters;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;

@Component
@ConfigurationPropertiesBinding
public class TimeoutConverter implements Converter<String, Timeout>{

	@Override
	@SneakyThrows
	public Timeout convert(String source) {
		return Timeout.of(Duration.parse(source).toMillis(), TimeUnit.MILLISECONDS);
	}

}
