package io.github.springboot.httpclient.web.headers;

import java.util.Enumeration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.utils.PatternUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Request headers collector that collect main headers that are to be forwarded
 * to client http requests further made by the application.
 */
@Slf4j
public class ForwardedRequestHeadersCollector implements RequestHeaderCollector {

    @Autowired
    private HttpClient5Config config;

    @Autowired
    @Qualifier("downHeaders")
    private ObjectProvider<RequestHeadersProviders.RequestHeadersStorage> downHeaders;

    @Override
    public boolean supports(String method, String uri, String headerName) {
    	return PatternUtils.matchesOne(headerName, config.getRequestConfigProperties(method, uri).getHeadersPropagation().getDown()) ;
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
