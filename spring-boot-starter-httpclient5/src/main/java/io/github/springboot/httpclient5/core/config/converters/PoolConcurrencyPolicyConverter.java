package io.github.springboot.httpclient5.core.config.converters;

import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;

//@Component
//@ConfigurationPropertiesBinding
public class PoolConcurrencyPolicyConverter implements Converter<String, PoolConcurrencyPolicy>{

	@Override
	@SneakyThrows
	public PoolConcurrencyPolicy convert(String source) {
		return PoolConcurrencyPolicy.valueOf(source) ; 
	}

}
