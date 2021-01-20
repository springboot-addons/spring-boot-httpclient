package io.github.springboot.httpclient.interceptors;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.springboot.httpclient.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.config.model.ConnectionConfiguration;
import io.github.springboot.httpclient.interceptors.headers.HeadersPropagationInterceptor;
import io.github.springboot.httpclient.interceptors.impl.ContentLengthHeaderRemoverInterceptor;
import io.github.springboot.httpclient.interceptors.impl.LoggingHttpRequestInterceptor;
import io.github.springboot.httpclient.interceptors.impl.TooManyRequestsHttpResponseInterceptor;
import lombok.Data;

@Configuration
public class HttpInterceptorsProviderConfig {

    @Autowired
    private HttpClientConfigurationHelper config;

    @Autowired
    private HeadersPropagationInterceptor headerInterceptor;

    @Bean
    public HttpInterceptorsProvider httpInterceptors() {
        final HttpInterceptorsProvider provider = new HttpInterceptorsProvider();
        provider.firstRequestInterceptors.add(new ContentLengthHeaderRemoverInterceptor(config));

        provider.requestInterceptors.add(headerInterceptor);

        final LoggingHttpRequestInterceptor loggingHttpRequestInterceptor = new LoggingHttpRequestInterceptor(config);
        provider.requestInterceptors.add(loggingHttpRequestInterceptor);
//        provider.requestInterceptors.add(new LegacyThrottlingHttpRequestInterceptor(config));

        provider.responseInterceptors.add(loggingHttpRequestInterceptor);
        provider.responseInterceptors.add(headerInterceptor);
        provider.responseInterceptors
                .add(new TooManyRequestsHttpResponseInterceptor(config, ConnectionConfiguration.DEFAULT_DELAY));
        return provider;
    }

    @Data
    public static class HttpInterceptorsProvider {
        private List<HttpResponseInterceptor> responseInterceptors = new ArrayList<>();
        private List<HttpRequestInterceptor> requestInterceptors = new ArrayList<>();
        private List<HttpResponseInterceptor> firstResponseInterceptors = new ArrayList<>();
        private List<HttpRequestInterceptor> firstRequestInterceptors = new ArrayList<>();
    }

}
