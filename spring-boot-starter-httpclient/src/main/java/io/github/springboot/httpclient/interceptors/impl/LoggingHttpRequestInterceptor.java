package io.github.springboot.httpclient.interceptors.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.springboot.httpclient.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.constants.ConfigurationConstants;
import io.github.springboot.httpclient.constants.HttpClientConstants;
import io.github.springboot.httpclient.interceptors.HttpClientInterceptor;
import io.github.springboot.httpclient.internal.CRFilteredOutputStream;
import io.github.springboot.httpclient.utils.HttpClientUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingHttpRequestInterceptor implements HttpClientInterceptor {

    public static final String BEGIN_TIME = "__BEGIN_TIME_LHRI";

    private final HttpClientConfigurationHelper config;

    /**
     * @param config
     */
    public LoggingHttpRequestInterceptor(final HttpClientConfigurationHelper config) {
        this.config = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final HttpRequest httprequest, final HttpContext httpcontext)
            throws HttpException, IOException {

        if (log.isInfoEnabled()) {
            httpcontext.setAttribute(BEGIN_TIME, System.currentTimeMillis());
            final URI uri = HttpClientUtils.getUri(httprequest, httpcontext);

            if (uri != null) {
                httpcontext.setAttribute(HttpClientConstants.URL_CONTEXT, uri.toString());
            }

            if (log.isDebugEnabled()) {
                log.debug(">>> Request on {}", uri);
                final HttpClientContext httpClientContext = HttpClientContext.adapt(httpcontext);
                final String globalCookiePolicy = config.getGlobalConfiguration(ConfigurationConstants.COOKIE_POLICY);
                if (!HttpClientUtils.isDisabledCookiePolicy(globalCookiePolicy)) {
                    final List<Cookie> cookies = httpClientContext.getCookieStore().getCookies();
                    log.debug(">>> Request CookieStore contains {} cookies of spec : {} ", cookies.size(),
                            httpClientContext.getCookieSpec() != null ? httpClientContext.getCookieSpec().getClass()
                                    : "UNKNOWN");
                    if (cookies != null && !cookies.isEmpty()) {
                        for (final Cookie cookie : cookies) {
                            log.debug(">>> Request Cookie {}", cookie.toString());
                        }
                    }
                } else {
                    log.debug(">>> Cookies have been explicitly disabled by global configuration");
                }
                log.debug(">>> Request Headers : {}", Arrays.toString(httprequest.getAllHeaders()));
            }

            final Boolean logPostMethods = config.getConfiguration(uri.toString(),
                    ConfigurationConstants.LOG_POST_METHODS);
            if (logPostMethods) {
                // Log complet des requetes POST
                final String requestLoggerName = HttpClientConstants.class.getName() + "." + uri.getHost() + "."
                        + uri.getPort();
                final Logger requestLogger = LoggerFactory.getLogger(requestLoggerName);

                if (requestLogger.isInfoEnabled()) {
                    if (httprequest instanceof HttpEntityEnclosingRequest) {
                        final HttpEntityEnclosingRequest post = (HttpEntityEnclosingRequest) httprequest;
                        final int entityLength = httprequest instanceof HttpPost
                                ? (int) post.getEntity().getContentLength()
                                : 0;
                        final ByteArrayOutputStream bos = entityLength > 0 ? new ByteArrayOutputStream(entityLength)
                                : new ByteArrayOutputStream();
                        if (post.getEntity() != null && post.getEntity().isRepeatable()) {
                            post.getEntity().writeTo(new CRFilteredOutputStream(bos));
                        } else {
                            bos.write("Entity unavailable".getBytes());
                        }
                        requestLogger.info(bos.toString());
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final HttpResponse httpresponse, final HttpContext httpcontext)
            throws HttpException, IOException {
        if (log.isInfoEnabled()) {

            final long end = System.currentTimeMillis();
            final HttpClientContext httpClientContext = HttpClientContext.adapt(httpcontext);
            final long time = end - (Long) httpClientContext.getAttribute(BEGIN_TIME);
            log.info("<<< HttpContext : {}", ToStringBuilder.reflectionToString(httpcontext));
            log.info("<<< Invocation of {} took {} ms, responseCode={}",
                    httpClientContext.getAttribute(HttpClientConstants.URL_CONTEXT), time,
                    httpresponse.getStatusLine().getStatusCode());
            final String globalCookiePolicy = config.getGlobalConfiguration(ConfigurationConstants.COOKIE_POLICY);

            if (log.isDebugEnabled()) {
                if (!HttpClientUtils.isDisabledCookiePolicy(globalCookiePolicy)) {
                    final List<Cookie> cookies = httpClientContext.getCookieStore().getCookies();
                    log.debug("<<< Response Cookie Store contains {} cookies ", cookies.size());
                    if (cookies != null && !cookies.isEmpty()) {
                        for (final Cookie cookie : cookies) {
                            log.debug("<<< Response Cookie {}", cookie.toString());
                        }
                    }
                } else {
                    log.debug("<<< Response cookies have been explicitly disabled by global configuration");
                }
                log.debug("<<< Response Headers : {}", Arrays.toString(httpresponse.getAllHeaders()));
                log.debug("<<< Response Entity content type : {}", httpresponse.getEntity().getContentType());
            }
        }
    }

}
