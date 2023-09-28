package io.github.springboot.httpclient5.core.config.converters;

import java.util.concurrent.TimeUnit;

import org.apache.hc.core5.util.TimeValue;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;

@Component
@ConfigurationPropertiesBinding
public class IntegerTimeValueConverter implements Converter<Integer, TimeValue>{

	@Override
	@SneakyThrows
	public TimeValue convert(Integer source) {
		return TimeValue.of(source, TimeUnit.MILLISECONDS);
	}

}
