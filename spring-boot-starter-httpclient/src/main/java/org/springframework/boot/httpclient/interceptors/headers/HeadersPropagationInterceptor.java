package org.springframework.boot.httpclient.interceptors.headers;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;

import javax.inject.Provider;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.httpclient.interceptors.headers.RequestHeadersProviders.RequestHeadersStorage;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HeadersPropagationInterceptor implements Filter, HttpRequestInterceptor, HttpResponseInterceptor {

    @Autowired(required = false)
    @Qualifier("downHeaders")
    private Provider<RequestHeadersStorage> downHeaders;

    @Autowired(required = false)
    @Qualifier("upHeaders")
    private Provider<RequestHeadersStorage> upHeaders;

    @Autowired
    private HeadersPropagationConfig config;

    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
        if (config.isEnabledPropagation()) {
            try {
                Arrays.stream(response.getAllHeaders())
                        .filter(h -> config.getUpPattern().matcher(h.getName()).matches()).forEach(h -> {
                            log.debug("*** Storing header {} from subsequent call", h);
                            upHeaders.get().add(h.getName(), h.getValue());
                        });
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        if (config.isEnabledPropagation()) {
            try {
                if (downHeaders.get() != null) {
                    downHeaders.get().getHeaderList().stream()
                            .filter(h -> config.getDownPattern().matcher(h.getName()).matches()).forEach(h -> {
                                log.debug("*** Using header {} from storage for subsequent call to {}", h,
                                        request.getRequestLine());
                                request.addHeader(h);
                            });
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest hRequest = (HttpServletRequest) request;
            Enumeration<String> headerNames = hRequest.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = (String) headerNames.nextElement();
                if (config.getDownPattern().matcher(headerName).matches()) {
                    log.debug("*** Storing incomming header {}", headerName, hRequest.getHeaders(headerName));
                    downHeaders.get().add(headerName, hRequest.getHeaders(headerName));
                }

            }

            HttpServletResponse hResponse = (HttpServletResponse) response;
            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(hResponse);
            chain.doFilter(hRequest, responseWrapper);

            upHeaders.get().getHeaderList().stream().filter(eh -> config.getUpPattern().matcher(eh.getName()).matches())
                    .forEach(eh -> {
                        log.debug("*** Forwarding header from storage {}", eh);
                        responseWrapper.addHeader(eh.getName(), eh.getValue());
                    });

            responseWrapper.copyBodyToResponse();

        } else {
            chain.doFilter(request, response);
        }
    }

}
