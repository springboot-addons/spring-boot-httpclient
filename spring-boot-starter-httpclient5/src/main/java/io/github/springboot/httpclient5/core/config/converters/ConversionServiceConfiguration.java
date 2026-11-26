package io.github.springboot.httpclient5.core.config.converters;

import java.util.Set;

import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;

@Configuration
public class ConversionServiceConfiguration {

	@Bean
	public ConversionService conversionService(Set<Converter<?, ?>> converters) {
		// Start from Spring Boot's ApplicationConversionService so that the Boot converters
		// (Duration, DataSize, Period, delimited strings, ...) remain available when this bean
		// becomes the conversion service of the application context.
		ApplicationConversionService conversionService = new ApplicationConversionService();
		converters.forEach(conversionService::addConverter);
		return conversionService;
	}
}
