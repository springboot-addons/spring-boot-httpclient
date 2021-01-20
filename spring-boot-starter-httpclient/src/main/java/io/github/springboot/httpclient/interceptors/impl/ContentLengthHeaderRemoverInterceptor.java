package io.github.springboot.httpclient.interceptors.impl;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import io.github.springboot.httpclient.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.constants.ConfigurationConstants;
import io.github.springboot.httpclient.utils.HttpClientUtils;

public class ContentLengthHeaderRemoverInterceptor implements HttpRequestInterceptor {

  private final HttpClientConfigurationHelper config;

  public ContentLengthHeaderRemoverInterceptor(final HttpClientConfigurationHelper config) {
    this.config = config;
  }

  @Override
  public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
    final String requestUri = HttpClientUtils.getUri(request, context).toString();
    if (requestUri != null) {
      final Boolean contentLenghtHasToBeRemoved = config.isTrue(requestUri.toString(),
          ConfigurationConstants.REMOVE_HEADERS);
      if (contentLenghtHasToBeRemoved) {
        request.removeHeaders(HTTP.CONTENT_LEN);
      }
      // fighting org.apache.http.protocol.RequestContent's
      // ProtocolException("Content-Length
      // header already present");
    }
  }
}