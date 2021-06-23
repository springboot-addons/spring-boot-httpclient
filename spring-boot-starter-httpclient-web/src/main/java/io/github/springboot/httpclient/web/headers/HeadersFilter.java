package io.github.springboot.httpclient.web.headers;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.ContentCachingResponseWrapper;

import io.github.springboot.httpclient.core.interceptors.headers.RequestHeadersProviders;
import lombok.extern.slf4j.Slf4j;

/**
 * Headers filter:
 * - collects incoming request headers (collectors)
 * - generated outgoing response headers (providers)
 */
@Slf4j
public class HeadersFilter implements Filter {

    @Autowired
    private ObjectProvider<RequestHeaderCollector> requestHeaderCollectors;

    @Autowired
    private ObjectProvider<ResponseHeaderProvider> responseHeaderProviders;

    @Autowired
    private ObjectProvider<RequestHeadersProviders.RequestHeadersStorage> headersStorages;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        // Initialization of request scope header storages
        headersStorages.forEach(RequestHeadersProviders.RequestHeadersStorage::clear);

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            Enumeration<String> headerNames = httpRequest.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                requestHeaderCollectors.stream().filter(c -> c.supports(headerName)).forEach(c -> c.handle(headerName, httpRequest.getHeaders(headerName)));
            }

            HttpServletResponse httpResponse = (HttpServletResponse) response;
            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);
            chain.doFilter(request, responseWrapper);

            responseHeaderProviders.stream().forEach(p -> p.getHeaderNames().forEach(n -> p.getHeaderValues(n).forEach(v -> responseWrapper.addHeader(n, v))));

            responseWrapper.copyBodyToResponse();
        } else {
            chain.doFilter(request, response);
        }
    }

}
