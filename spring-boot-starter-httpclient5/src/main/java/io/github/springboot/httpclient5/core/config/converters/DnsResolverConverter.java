package io.github.springboot.httpclient5.core.config.converters;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.impl.InMemoryDnsResolver;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;

@Component
@ConfigurationPropertiesBinding
public class DnsResolverConverter implements Converter<String, DnsResolver>{

	@Override
	@SneakyThrows
	public DnsResolver convert(String source) {
		DnsResolver r = null ; 
		if ("SYSTEM".equalsIgnoreCase(source)) {
			r = new SystemDefaultDnsResolver() ;
		}
		else if ("IN_MEMORY".equalsIgnoreCase(source)) {
			r = new InMemoryDnsResolver() ;
		}
		else {
			r = (DnsResolver) Class.forName(source).getDeclaredConstructor().newInstance() ;
		} 
		return r ;
	}

}
