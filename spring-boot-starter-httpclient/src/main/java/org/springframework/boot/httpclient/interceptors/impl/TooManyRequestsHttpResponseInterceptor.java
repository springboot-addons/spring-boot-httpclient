package org.springframework.boot.httpclient.interceptors.impl;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;
import org.springframework.boot.httpclient.config.HttpClientConfigurationHelper;
import org.springframework.boot.httpclient.config.model.HostConfiguration;
import org.springframework.boot.httpclient.constants.HttpClientConstants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TooManyRequestsHttpResponseInterceptor implements HttpResponseInterceptor {

    private static final String RETRY_AFTER_HEADER_NAME = "Retry-After";
    private Double defaultDelay;
    private HttpClientConfigurationHelper config;

    /**
     * @param config
     */

    public TooManyRequestsHttpResponseInterceptor(int defaultDelay) {
    }

    public TooManyRequestsHttpResponseInterceptor(final HttpClientConfigurationHelper config, Double defaultDelay) {
        this.config = config;
        this.defaultDelay = defaultDelay * 1000;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final HttpResponse httpresponse, final HttpContext httpcontext)
            throws HttpException, IOException {
        if (httpresponse.getStatusLine().getStatusCode() == HttpClientConstants.SC_TOO_MANY_REQUESTS) {
            final HttpClientContext clientContext = HttpClientContext.adapt(httpcontext);
            // final HttpRequest request = clientContext.getRequest();
            String host = clientContext.getTargetHost().toHostString();
            // String requestLine = request.getRequestLine().getUri();

            final Header retryHeader = httpresponse.getFirstHeader(RETRY_AFTER_HEADER_NAME);
            Double retryDelay = null;
            if (retryHeader != null) {
                try {
                    retryDelay = Double.parseDouble(retryHeader.getValue()) * 1000;
                } catch (final Exception ignore) {
                }
            }

            // String key = config.getConfigurationKeyForRequestUri(uri);
            HostConfiguration configuration = config.getUniqueConfigurationForHostname(host);

            if (retryDelay == null && configuration != null) {
                retryDelay = (Double) configuration.getConnection().getDelayBeforeRetrying() * 1000;
            }
            synchronized (configuration) {
                try {
                    Double max = Math.max(retryDelay, defaultDelay);
                    log.warn("Waiting for {} ms; Targetted Host respond with {}", max,
                            httpresponse.getStatusLine().getStatusCode());
                    configuration.wait(max.longValue());
                } catch (final InterruptedException e) {
                    log.warn("InterruptedException on TooManyRequests waiting", e);
                }
            }

            throw new IOException("Too many requests");
        }
    }
}
