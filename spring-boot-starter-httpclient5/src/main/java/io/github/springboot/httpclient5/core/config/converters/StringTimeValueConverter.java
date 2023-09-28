package io.github.springboot.httpclient5.core.config.converters;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.hc.core5.util.TimeValue;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;

@Component
@ConfigurationPropertiesBinding
public class StringTimeValueConverter implements Converter<String, TimeValue>{

	@Override
	@SneakyThrows
	public TimeValue convert(String source) {
		return TimeValue.of(Duration.parse(source).toMillis(), TimeUnit.MILLISECONDS);
	}

}
