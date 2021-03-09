package io.github.springboot.httpclient.core.interceptors.impl;

//package io.github.springboot.httpclient.core.interceptors.impl;
//
//import java.io.IOException;
//
//import org.apache.http.HttpException;
//import org.apache.http.HttpRequest;
//import org.apache.http.HttpRequestInterceptor;
//import org.apache.http.protocol.HttpContext;
//import io.github.springboot.httpclient.core.config.HttpClientConfigurationHelper;
//import io.github.springboot.httpclient.core.constants.ConfigurationConstants;
//import io.github.springboot.httpclient.core.utils.HttpClientUtils;
//
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//public class LegacyThrottlingHttpRequestInterceptor implements HttpRequestInterceptor {
//
//  private final HttpClientConfigurationHelper config;
//
//  /**
//   * @param config
//   */
//  public LegacyThrottlingHttpRequestInterceptor(final HttpClientConfigurationHelper config) {
//    this.config = config;
//  }
//
//  /**
//   * {@inheritDoc}
//   */
//  @Override
//  public void process(final HttpRequest httprequest, final HttpContext httpcontext) throws HttpException, IOException {
//	String requestUri = HttpClientUtils.getUri(httprequest, httpcontext).toString();
//    final String requestMethod = httprequest.getRequestLine().getMethod();
//
//    com.google.common.util.concurrent.RateLimiter limiter = config.getConfiguration(requestUri, requestMethod, ConfigurationConstants.THROTTLING_POLICY ) ;
//    if (limiter != null) {
//        log.debug("RateLimiter hash : {} ", limiter.hashCode());
//      double waitingTime = limiter.acquire() ;
//      if (waitingTime<= 0) {
//        log.debug("No waiting...req {}/sec is not reached", limiter.getRate());
//      }else {
//          log.warn("Waiting for {} ms, because rate limit for url {} is {}/s req" , waitingTime, requestUri, limiter.getRate());
//      }
//
//    }
//  }
//}
