package io.github.springboot.httpclient5.core.config.converters;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;

@Configuration
public class ConversionServiceConfiguration {
	@Bean
	public ConversionService conversionService(@Autowired Set<Converter> converters) {
	    ConversionServiceFactoryBean factory = new ConversionServiceFactoryBean();
	    factory.setConverters(converters);
	    factory.afterPropertiesSet();
	    return factory.getObject();
	}
}

