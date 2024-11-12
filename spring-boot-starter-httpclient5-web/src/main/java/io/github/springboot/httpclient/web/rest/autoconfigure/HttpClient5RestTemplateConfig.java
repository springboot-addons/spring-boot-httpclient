package io.github.springboot.httpclient.web.rest.autoconfigure;

import org.apache.hc.client5.http.classic.HttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Configuration
@ConditionalOnProperty(name = "spring.httpclient5.web.rest-template.enabled", havingValue = "true", matchIfMissing = true)
public class HttpClient5RestTemplateConfig {

	@Bean
	@Primary
	public RestTemplateBuilder restTemplateBuilder(HttpClient httpClient) {
		RestTemplateBuilder builder = new RestTemplateBuilder();
		builder.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient));
		return builder;
	}
}
