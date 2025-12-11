package io.github.springboot.httpclient.web.rest.autoconfigure;

import org.apache.hc.client5.http.classic.HttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.restclient.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Configuration
@ConditionalOnProperty(name = "spring.httpclient5.web.rest-template.enabled", havingValue = "true", matchIfMissing = true)
public class HttpClient5RestTemplateConfig {

	@Bean
	@Primary
	public RestTemplateCustomizer hc5RestTemplateCustomizer(HttpClient httpClient) {
		return rt -> {
			rt.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
			
		} ;
	}
}
