package io.github.springboot.httpclient.core.interceptors.impl;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import io.github.springboot.httpclient.core.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.core.constants.ConfigurationConstants;
import io.github.springboot.httpclient.core.utils.HttpClientUtils;

public class HeaderRemoverInterceptor implements HttpRequestInterceptor {

	private final HttpClientConfigurationHelper config;

	public HeaderRemoverInterceptor(final HttpClientConfigurationHelper config) {
		this.config = config;
	}

	@Override
	public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
		final String requestUri = HttpClientUtils.getUri(request, context).toString();
		if (requestUri != null) {
			final List<String> headersToRemove = config.getConfiguration(requestUri.toString(),
					ConfigurationConstants.REMOVE_HEADERS);
			if (headersToRemove != null) {
				headersToRemove.forEach(request::removeHeaders);
			}
		}
	}
}
