package org.springframework.boot.httpclient.internal;

import java.util.List;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.httpclient.auth.cas.CasAuthenticator;
import org.springframework.boot.httpclient.config.HttpClientConfigurationHelper;
import org.springframework.boot.httpclient.config.model.Authentication;
import org.springframework.boot.httpclient.config.model.ProxyConfiguration;
import org.springframework.boot.httpclient.constants.ConfigurationConstants;
import org.springframework.boot.httpclient.utils.HttpClientUtils;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RequestConfigurer {

  @Autowired
  private HttpClientConfigurationHelper config;

  @Autowired(required = false)
  private CasAuthenticator casAuth;

  public void configureRequest(HttpHost target, HttpRequest request, HttpContext context) {
    try {
      final String requestUri = HttpClientUtils.getUri(request, null).toString();
      final RequestLine requestLine = request.getRequestLine();
      final String requestMethod = requestLine.getMethod();
      final Integer socketTimeout = config.getConfiguration(requestUri, requestMethod, ConfigurationConstants.SOCKET_TIMEOUT);
      final Integer connectTimeout = config.getConfiguration(requestUri, requestMethod, ConfigurationConstants.CONNECTION_TIMEOUT);
      final RequestConfig.Builder requestConfig = RequestConfig.custom()
    		  .setSocketTimeout(socketTimeout)
    		  .setConnectTimeout(connectTimeout);
      final String cookiePolicy = config.getConfiguration(requestUri, ConfigurationConstants.COOKIE_POLICY);

      final String cookieSpec = getCookieSpec(cookiePolicy);
      if (StringUtils.isNotBlank(cookieSpec)) {
        requestConfig.setCookieSpec(cookieSpec);
      }

      final List<String> removeHeaders = config.getConfiguration(requestUri, ConfigurationConstants.REMOVE_HEADERS);
      if (removeHeaders != null && !removeHeaders.isEmpty()) {
        for (final String header : removeHeaders) {
          if (request.getFirstHeader(header) != null) {
            MethodUtils.invokeMethod(request, "removeHeaders", header);
          }
        }
      }

      final String authentication = config.getConfiguration(requestUri, requestMethod,
          ConfigurationConstants.AUTHENTICATION_AUTH_TYPE);
      final String authenticationEndPoint = config.getConfiguration(requestUri, requestMethod,
          ConfigurationConstants.AUTHENTICATION_DOMAIN);

      if (Authentication.AUTH_TYPE_CAS.equals(authentication) && casAuth != null) {
        casAuth.authCas(request, authenticationEndPoint);
      }

      // Proxy
      ProxyConfiguration proxyConfiguration = config.getProxyConfiguration(requestUri);
      if (proxyConfiguration != null) {
        final String httpProxyHost = proxyConfiguration.getHost();
        final Integer httpProxyPort = proxyConfiguration.getPort();
        log.debug("Using proxy {}:{} for {}", httpProxyHost, httpProxyPort, requestUri);
        final HttpHost proxyHost = new HttpHost(httpProxyHost, httpProxyPort);
        requestConfig.setProxy(proxyHost);

      }

      MethodUtils.invokeMethod(request, "setConfig", requestConfig.build());
      // Compression
      final String compression = config.getConfiguration(requestUri, requestMethod, ConfigurationConstants.COMPRESSION);
      if (StringUtils.isNotBlank(compression) && request.getFirstHeader(HttpHeaders.ACCEPT_ENCODING) == null) {
        log.debug("Using header '{}: {}' for {}", HttpHeaders.ACCEPT_ENCODING, compression, requestUri);
        MethodUtils.invokeMethod(request, "addHeader", new Object[] { HttpHeaders.ACCEPT_ENCODING, compression });
      }
    } catch (final Exception e) {
      log.warn("Unable to configure httpclient request, no uri available : using defaut configuration", e);
    }
  }

  private String getCookieSpec(final String cookiePolicy) {
    String cookieSpec = null;
    if (StringUtils.equalsIgnoreCase(cookiePolicy, CookieSpecs.DEFAULT)) {
      cookieSpec = CookieSpecs.DEFAULT;
    } else if (StringUtils.equalsIgnoreCase(cookiePolicy, CookieSpecs.STANDARD)) {
      cookieSpec = CookieSpecs.STANDARD;
    } else if (StringUtils.equalsIgnoreCase(cookiePolicy, CookieSpecs.NETSCAPE)) {
      cookieSpec = CookieSpecs.NETSCAPE;
    } else if (StringUtils.equalsIgnoreCase(cookiePolicy, CookieSpecs.STANDARD_STRICT)) {
      cookieSpec = CookieSpecs.STANDARD_STRICT;
    } else if (StringUtils.equalsIgnoreCase(cookiePolicy, CookieSpecs.IGNORE_COOKIES)) {
      cookieSpec = CookieSpecs.IGNORE_COOKIES;
    }
    return cookieSpec;
  }
}
