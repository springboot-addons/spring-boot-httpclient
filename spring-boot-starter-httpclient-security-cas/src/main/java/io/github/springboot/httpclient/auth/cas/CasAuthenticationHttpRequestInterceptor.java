package io.github.springboot.httpclient.auth.cas;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.RequestLine;
import org.apache.http.protocol.HttpContext;

import io.github.springboot.httpclient.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.constants.ConfigurationConstants;
import io.github.springboot.httpclient.constants.HttpClientConstants;
import io.github.springboot.httpclient.utils.HttpClientUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CasAuthenticationHttpRequestInterceptor implements HttpRequestInterceptor {

	private final HttpClientConfigurationHelper config;
	private CasAuthenticator casAuth;

	/**
	 * @param config
	 */
	public CasAuthenticationHttpRequestInterceptor(final HttpClientConfigurationHelper config,
			CasAuthenticator casAuth) {
		this.config = config;
		this.casAuth = casAuth;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(final HttpRequest request, final HttpContext httpcontext) throws HttpException, IOException {
		final String requestUri = HttpClientUtils.getUri(request, null).toString();
		final RequestLine requestLine = request.getRequestLine();
		final String requestMethod = requestLine.getMethod();

		final String authentication = config.getConfiguration(requestUri, requestMethod,
				ConfigurationConstants.AUTHENTICATION_AUTH_TYPE);
		final String authenticationEndPoint = config.getConfiguration(requestUri, requestMethod,
				ConfigurationConstants.AUTHENTICATION_DOMAIN);

		if (HttpClientConstants.CAS_AUTHENTIFICATION_SCHEME.equals(authentication) && casAuth != null) {
			try {
				casAuth.authCas(request, authenticationEndPoint);
			} catch (Exception e) {
				throw new HttpException("Unable to authenticate through CAS");
			}
		}
	}
}
