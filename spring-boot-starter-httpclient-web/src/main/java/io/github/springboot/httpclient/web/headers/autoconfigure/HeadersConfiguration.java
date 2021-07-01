package io.github.springboot.httpclient.web.headers.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.springboot.httpclient.web.headers.ForwardedRequestHeadersCollector;
import io.github.springboot.httpclient.web.headers.HeadersFilter;
import io.github.springboot.httpclient.web.headers.CollectedResponseHeadersProvider;

@Configuration
public class HeadersConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "httpclient.web.headers-filter", name = "enabled", havingValue = "true", matchIfMissing = true)
    public HeadersFilter headersFilter() {
        return new HeadersFilter();
    }

    @Configuration
    @ConditionalOnProperty(prefix = "httpclient.web.headers-propagation", name = "enabled", havingValue = "true", matchIfMissing = true)
    public static class HeadersPropagationConfiguration {

        @Bean
        public CollectedResponseHeadersProvider collectedResponseHeadersProvider() {
            return new CollectedResponseHeadersProvider();
        }

        @Bean
        public ForwardedRequestHeadersCollector forwardedRequestHeadersCollector() {
            return new ForwardedRequestHeadersCollector();
        }
    }
}
