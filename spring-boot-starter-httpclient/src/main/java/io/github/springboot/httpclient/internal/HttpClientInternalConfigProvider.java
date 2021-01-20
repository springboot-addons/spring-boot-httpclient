package io.github.springboot.httpclient.internal;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

import io.github.springboot.httpclient.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.config.model.Authentication;
import io.github.springboot.httpclient.config.model.HostConfiguration;
import io.github.springboot.httpclient.config.model.ProxyConfiguration;
import io.github.springboot.httpclient.constants.ConfigurationConstants;
import io.github.springboot.httpclient.constants.HttpClientConstants;
import io.github.springboot.httpclient.utils.HostUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class HttpClientInternalConfigProvider {
	private static final int ONE_SECOND_IN_MILLIS = 1000;
	private static final int DEFAULT_KEEP_ALIVE_TIME_MILLIS = 5 * ONE_SECOND_IN_MILLIS;

	@Autowired
	protected HttpClientConfigurationHelper config;

	@Bean
	// TODO Profil spring & CookieStoreProvider in own class
	@ConditionalOnProperty(name = "junit.testcase", havingValue = "true")
	public CookieStore threadLocalCookieStore() {
		return new ThreadLocalCookieStore();
	}

	@Bean
	@ConditionalOnWebApplication
	@ConditionalOnProperty(name = "junit.testcase", havingValue = "false", matchIfMissing = true)
	@RequestScope(proxyMode = ScopedProxyMode.INTERFACES)
	public CookieStore requestCookieStore() {
		return new BasicCookieStore();
	}

	@Bean
	public Registry<AuthSchemeProvider> getAuthSchemeProviders(List<NamedAuthSchemeProvider> authSchemeProviders) {
		final RegistryBuilder<AuthSchemeProvider> registryBuilder = RegistryBuilder.<AuthSchemeProvider>create() ;
		authSchemeProviders.forEach(n -> {
			registryBuilder.register(n.getName(), n.getProvider()) ;
		}) ;

		return registryBuilder.build();
	}

	@Bean
	// TODO CredentialsProviderCustomizer
	public CredentialsProvider getCredentialsProvider() {
		final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

		for (final HostConfiguration h : config.getAllConfigurations().getHosts().values()) {
			final String baseUri = h.getBaseUrl();

			// Set credential par host
			final Boolean authRequired = config.isTrue(baseUri, ConfigurationConstants.AUTHENTICATION_REQUIRED);
			if (authRequired) {
				final String httpUser = config.getConfiguration(baseUri, ConfigurationConstants.AUTHENTICATION_USER);
				final String httpPassword = config.getConfiguration(baseUri,
						ConfigurationConstants.AUTHENTICATION_PASSWORD);
				final String httpDomain = config.getConfiguration(baseUri,
						ConfigurationConstants.AUTHENTICATION_DOMAIN);
				final String authType = config.getConfiguration(baseUri,
						ConfigurationConstants.AUTHENTICATION_AUTH_TYPE);

				Credentials credentials = null;
				if (AuthSchemes.NTLM.equalsIgnoreCase(authType)) {
					credentials = new NTCredentials(httpUser, httpPassword, getLocalhostName(), httpDomain);
				} else if (AuthSchemes.BASIC.equalsIgnoreCase(authType)) {
					credentials = new UsernamePasswordCredentials(httpUser, httpPassword);
				}

				if (credentials != null) {
					URI uri;
					try {
						uri = new URI(baseUri);
						credentialsProvider.setCredentials(
								new AuthScope(uri.getHost(), HostUtils.getPort(uri), AuthScope.ANY_REALM, authType),
								credentials);
					} catch (final URISyntaxException e) {
						log.warn("unable to configure credential provider for baseurl : " + baseUri, e);
					}
				} else {
					log.info(
							"CredentialsProvider for {} is NULL, maybe the authType {} is not supported, check config file",
							baseUri, authType);
				}
			}

			// Set credential par host for proxy
			if (config.useProxyForHost(baseUri)) {
				final ProxyConfiguration proxyConfiguration = config.getProxyConfiguration(baseUri);
				if (proxyConfiguration.getAuthentification().isRequired()) {
					final String proxyHost = proxyConfiguration.getHost();
					final Integer proxyPort = proxyConfiguration.getPort();
					final String proxyHttpUser = proxyConfiguration.getAuthentification().getUser();
					final String proxyHttpPassword = proxyConfiguration.getAuthentification().getPassword();
					final String proxyAuthType = proxyConfiguration.getAuthentification().getAuthType();

					Credentials proxyCrendentials = null;
					if (AuthSchemes.NTLM.equalsIgnoreCase(proxyAuthType)) {
						final String proxyHttpDomain = proxyConfiguration.getAuthentification().getDomain();
						proxyCrendentials = new NTCredentials(proxyHttpUser, proxyHttpPassword, getLocalhostName(),
								proxyHttpDomain);
					} else {
						proxyCrendentials = new UsernamePasswordCredentials(proxyHttpUser, proxyHttpPassword);
					}
					credentialsProvider.setCredentials(
							new AuthScope(proxyHost, proxyPort, AuthScope.ANY_REALM, proxyAuthType), proxyCrendentials);
				}
			}

		}
		return credentialsProvider;
	}

	private String getLocalhostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (final UnknownHostException e) {
			return "localhost";
		}
	}

	@Bean
	public RequestConfig getDefaultRequestConfig() {
		final Integer poolTimeout = config.getGlobalConfiguration(ConfigurationConstants.POOL_TIMEOUT);
		final int socketTimeout = config.getGlobalConfiguration(ConfigurationConstants.SOCKET_TIMEOUT);
		final int connectTimeout = config.getGlobalConfiguration(ConfigurationConstants.CONNECTION_TIMEOUT);
		final String cookiePolicy = config.getGlobalConfiguration(ConfigurationConstants.COOKIE_POLICY);

		final Builder builder = RequestConfig.custom().setConnectionRequestTimeout(poolTimeout)
				.setConnectTimeout(connectTimeout).setSocketTimeout(socketTimeout);
		if (!config.isCookieManagementDisabled()) {
			builder.setCookieSpec(StringUtils.isNotBlank(cookiePolicy) ? cookiePolicy : CookieSpecs.DEFAULT);
		}

		if (System.getProperty(HttpClientConstants.KERBEROS_PARAM_PROPERTY) != null) {
			builder.setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.BASIC,
					AuthSchemes.KERBEROS, AuthSchemes.SPNEGO, AuthSchemes.DIGEST, AuthSchemes.CREDSSP))
					.setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.BASIC,
							AuthSchemes.KERBEROS, AuthSchemes.SPNEGO, AuthSchemes.DIGEST, AuthSchemes.CREDSSP));
		} else {
			builder.setTargetPreferredAuthSchemes(
					Arrays.asList(AuthSchemes.NTLM, AuthSchemes.BASIC, AuthSchemes.DIGEST, AuthSchemes.CREDSSP))
					.setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.BASIC, AuthSchemes.DIGEST,
							AuthSchemes.CREDSSP));
		}

		return builder.build();
	}

	@Bean
	public ConnectionKeepAliveStrategy connectionKeepAliveStrategy() {
		return new ConnectionKeepAliveStrategy() {
			@Override
			public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
				final HeaderElementIterator it = new BasicHeaderElementIterator(
						response.headerIterator(HTTP.CONN_KEEP_ALIVE));
				while (it.hasNext()) {
					final HeaderElement he = it.nextElement();
					final String param = he.getName();
					final String value = he.getValue();

					if (value != null && param.equalsIgnoreCase("timeout")) {
						return Long.parseLong(value) * ONE_SECOND_IN_MILLIS;
					}
				}
				return DEFAULT_KEEP_ALIVE_TIME_MILLIS;
			}
		};
	}
}
