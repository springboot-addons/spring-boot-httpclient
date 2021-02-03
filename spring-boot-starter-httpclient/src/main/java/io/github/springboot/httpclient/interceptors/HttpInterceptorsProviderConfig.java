package io.github.springboot.httpclient.interceptors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import io.github.springboot.httpclient.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.config.model.ConnectionConfiguration;
import io.github.springboot.httpclient.interceptors.headers.HeadersPropagationInterceptor;
import io.github.springboot.httpclient.interceptors.impl.HeaderRemoverInterceptor;
import io.github.springboot.httpclient.interceptors.impl.LoggingHttpRequestInterceptor;
import io.github.springboot.httpclient.interceptors.impl.TooManyRequestsHttpResponseInterceptor;

@Configuration
public class HttpInterceptorsProviderConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 100)
    @ConditionalOnProperty(prefix = "httpclient.core.interceptors.header-remover", 
    	name = "enabled", havingValue = "true", matchIfMissing = true)
    public HeaderRemoverInterceptor headerRemoverInterceptor(HttpClientConfigurationHelper config) {
        return new HeaderRemoverInterceptor(config);
    }
    
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 200)
    @ConditionalOnProperty(prefix = "httpclient.core.interceptors.headers-propagation", 
    	name = "enabled", havingValue = "true", matchIfMissing = true)
    public HeadersPropagationInterceptor headersPropagationInterceptor(HttpClientConfigurationHelper config) {
        return new HeadersPropagationInterceptor();
    }
    
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE - 100)
    @ConditionalOnProperty(prefix = "httpclient.core.interceptors.logging", 
    	name = "enabled", havingValue = "true", matchIfMissing = true)
    public LoggingHttpRequestInterceptor loggingHttpRequestInterceptor(HttpClientConfigurationHelper config) {
        return new LoggingHttpRequestInterceptor(config);
    }
    
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE - 200)
    @ConditionalOnProperty(prefix = "httpclient.core.interceptors.too-many-request-protection", 
    	name = "enabled", havingValue = "true", matchIfMissing = true)
    public TooManyRequestsHttpResponseInterceptor tooManyRequestsHttpResponseInterceptor(HttpClientConfigurationHelper config) {
        return new TooManyRequestsHttpResponseInterceptor(config, ConnectionConfiguration.DEFAULT_DELAY);
    }

}
