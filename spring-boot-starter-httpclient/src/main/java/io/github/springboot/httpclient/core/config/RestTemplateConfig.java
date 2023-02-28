package io.github.springboot.httpclient.core.config;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import io.github.springboot.httpclient.core.backport.HttpComponents4BackportClientHttpRequestFactory;

@Configuration
@ConditionalOnProperty(name = "httpclient.core.rest-template-httpclient4.enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnClass(RestTemplate.class)
public class RestTemplateConfig {

    @Autowired
    CloseableHttpClient httpClient;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(httpclient4RequestFactory());
    }

    @Bean
    public HttpComponents4BackportClientHttpRequestFactory httpclient4RequestFactory() {
    	HttpComponents4BackportClientHttpRequestFactory clientHttpRequestFactory = new HttpComponents4BackportClientHttpRequestFactory();
        clientHttpRequestFactory.setHttpClient(httpClient);
        return clientHttpRequestFactory;
    }
}