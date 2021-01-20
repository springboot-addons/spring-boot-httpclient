package org.springframework.boot.httpclient.interceptors.impl;

//package org.springframework.boot.httpclient.interceptors;
//
//import java.io.IOException;
//
//import org.apache.http.HeaderElement;
//import org.apache.http.HttpEntity;
//import org.apache.http.HttpException;
//import org.apache.http.HttpRequest;
//import org.apache.http.HttpResponse;
//import org.apache.http.client.entity.BrotliDecompressingEntity;
//import org.apache.http.protocol.HttpContext;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import org.springframework.boot.httpclient.HttpClientConstants;
//import org.springframework.boot.httpclient.config.hierarchical.HttpClientConfigurationHelper;
//import org.springframework.boot.httpclient.internal.HttpClientInterceptor;
//
//public class BrotliDeflaterContentEncodingInterceptor implements HttpClientInterceptor {
//
//  private static final Logger LOGGER = LoggerFactory.getLogger(BrotliDeflaterContentEncodingInterceptor.class);
//
//  private final HttpClientConfigurationHelper config;
//
//  /**
//   * @param config
//   */
//  public BrotliDeflaterContentEncodingInterceptor(final HttpClientConfigurationHelper config) {
//    this.config = config;
//  }
//
//  /**
//   * {@inheritDoc}
//   */
//  @Override
//  public void process(final HttpRequest httprequest, final HttpContext httpcontext) throws HttpException, IOException {
//    if (!httprequest.containsHeader(HttpClientConstants.ACCEPT_ENCODING)) {
//      httprequest.addHeader(HttpClientConstants.ACCEPT_ENCODING, HttpClientConstants.ACCEPT_ENCODING_GZIP);
//    }
//  }
//
//  /**
//   * {@inheritDoc}
//   */
//  @Override
//  public void process(final HttpResponse response, final HttpContext httpcontext) throws HttpException, IOException {
//    HttpEntity entity = response.getEntity();
//    if (entity != null) {
//      org.apache.http.Header ceheader = entity.getContentEncoding();
//      if (ceheader != null) {
//        HeaderElement[] codecs = ceheader.getElements();
//        for (int i = 0; i < codecs.length; i++) {
//          if (codecs[i].getName().equalsIgnoreCase(HttpClientConstants.CONTENT_ENCODING_BROTLI)) {
//            response.setEntity(new BrotliDecompressingEntity(response.getEntity()));
//            return;
//          }
//        }
//      }
//    }
//  }
//}
