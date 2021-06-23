package io.github.springboot.httpclient.web.headers;

import java.util.Enumeration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.github.springboot.httpclient.core.interceptors.headers.HeadersPropagationConfig;
import io.github.springboot.httpclient.core.interceptors.headers.RequestHeadersProviders;
import lombok.extern.slf4j.Slf4j;

/**
 * Request headers collector that collect main headers that are to be forwarded
 * to client http requests further made by the application.
 */
@Slf4j
public class ForwardedRequestHeadersCollector implements RequestHeaderCollector {

    @Autowired
    private HeadersPropagationConfig config;

    @Autowired
    @Qualifier("downHeaders")
    private ObjectProvider<RequestHeadersProviders.RequestHeadersStorage> downHeaders;

    @Override
    public boolean supports(String headerName) {
        return config.getDownPattern().matcher(headerName).matches();
    }

    @Override
    public void handle(String headerName, String headerValue) {
        downHeaders.ifAvailable(s -> s.add(headerName, headerValue));
    }

    @Override
    public void handle(String headerName, Enumeration<String> headerValues) {
        downHeaders.ifAvailable(s -> s.add(headerName, headerValues));
    }

}
