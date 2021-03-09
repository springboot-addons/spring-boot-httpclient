package io.github.springboot.httpclient.interceptors.impl;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.RequestLine;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;

import io.github.springboot.httpclient.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.config.model.ProxyConfiguration;
import io.github.springboot.httpclient.constants.ConfigurationConstants;
import io.github.springboot.httpclient.constants.HttpClientConstants;
import io.github.springboot.httpclient.utils.HttpClientUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpRequestConfigurerInterceptor implements HttpRequestInterceptor {

	private HttpClientConfigurationHelper config;

	public HttpRequestConfigurerInterceptor(HttpClientConfigurationHelper config) {
		this.config = config;
	}

	@Override
	public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
		try {
			final String requestUri = HttpClientUtils.getUri(request, context).toString();
			final RequestLine requestLine = request.getRequestLine();
			final String requestMethod = requestLine.getMethod();
			final Integer socketTimeout = config.getConfiguration(requestUri, requestMethod,
					ConfigurationConstants.SOCKET_TIMEOUT);
			final Integer connectTimeout = config.getConfiguration(requestUri, requestMethod,
					ConfigurationConstants.CONNECTION_TIMEOUT);
			
			final RequestConfig.Builder requestConfig = RequestConfig.custom()
					.setSocketTimeout(socketTimeout != null ? socketTimeout : HttpClientConstants.DEFAULT_SOCKET_TIMEOUT)
					.setConnectTimeout(connectTimeout != null ? connectTimeout : HttpClientConstants.DEFAULT_CONNECTION_TIMEOUT);

			final String cookiePolicy = config.getConfiguration(requestUri, ConfigurationConstants.COOKIE_POLICY);

			final String cookieSpec = getCookieSpec(cookiePolicy);
			if (StringUtils.isNotBlank(cookieSpec)) {
				requestConfig.setCookieSpec(cookieSpec);
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
			context.setAttribute(HttpClientContext.REQUEST_CONFIG, requestConfig.build());

			// Compression
			final String compression = config.getConfiguration(requestUri, requestMethod,
					ConfigurationConstants.COMPRESSION);
			if (StringUtils.isNotBlank(compression) && request.getFirstHeader(HttpHeaders.ACCEPT_ENCODING) == null) {
				log.debug("Using header '{}: {}' for {}", HttpHeaders.ACCEPT_ENCODING, compression, requestUri);
				request.addHeader(HttpHeaders.ACCEPT_ENCODING, compression);
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
