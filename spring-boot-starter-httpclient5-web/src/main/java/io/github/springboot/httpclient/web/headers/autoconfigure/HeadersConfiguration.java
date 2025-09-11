package io.github.springboot.httpclient.web.headers.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.springboot.httpclient.web.headers.CollectedResponseHeadersProvider;
import io.github.springboot.httpclient.web.headers.ForwardedRequestHeadersCollector;
import io.github.springboot.httpclient.web.headers.HeadersFilter;
import io.github.springboot.httpclient.web.headers.HeadersPropagationInterceptor;

@Configuration
public class HeadersConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "spring.httpclient5.web.headers-filter", name = "enabled", havingValue = "true", matchIfMissing = true)
    public HeadersFilter hc5HeadersFilter() {
        return new HeadersFilter();
    }

    @Configuration
    @ConditionalOnProperty(prefix = "spring.httpclient5.web.headers-propagation", name = "enabled", havingValue = "true", matchIfMissing = true)
    public static class HeadersPropagationConfiguration {

        @Bean
        public CollectedResponseHeadersProvider hc5CollectedResponseHeadersProvider() {
            return new CollectedResponseHeadersProvider();
        }

        @Bean
        public ForwardedRequestHeadersCollector hc5ForwardedRequestHeadersCollector() {
            return new ForwardedRequestHeadersCollector();
        }
        
        @Bean 
        public HeadersPropagationInterceptor hc5HeadersPropagationInterceptor() {
        	return new HeadersPropagationInterceptor() ;
        }
    }
}
