package io.github.springboot.httpclient5.core.config.converters;

import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import io.github.springboot.httpclient5.core.config.model.SimplePredefinedCredentialsProvider;
import lombok.SneakyThrows;

@Component
@ConfigurationPropertiesBinding
public class StringCredentialsProviderConverter implements Converter<String, CredentialsProvider>{

	@Override
	@SneakyThrows
	public CredentialsProvider convert(String source) {
		return new SimplePredefinedCredentialsProvider(source);
	}

}
