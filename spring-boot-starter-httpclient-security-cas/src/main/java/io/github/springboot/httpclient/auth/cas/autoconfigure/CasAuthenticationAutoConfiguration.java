package io.github.springboot.httpclient.auth.cas.autoconfigure;

import org.apache.http.HttpRequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.springboot.httpclient.auth.cas.CasAuthenticationHttpRequestInterceptor;
import io.github.springboot.httpclient.auth.cas.CasAuthenticator;
import io.github.springboot.httpclient.config.HttpClientConfigurationHelper;

@Configuration
public class CasAuthenticationAutoConfiguration {

	@Bean
	@ConditionalOnWebApplication
	@ConditionalOnClass(name = "org.jasig.cas.client.util.AssertionHolder")
	@ConditionalOnProperty(name = "httpclient.core.auth.cas.enabled", havingValue = "true", matchIfMissing = true)
	public CasAuthenticator casAuthenticator() {
		return new CasAuthenticator();
	}

	@Bean
	@ConditionalOnWebApplication
	@ConditionalOnClass(name = "org.jasig.cas.client.util.AssertionHolder")
	@ConditionalOnProperty(name = "httpclient.core.auth.cas.enabled", havingValue = "true", matchIfMissing = true)
	public HttpRequestInterceptor casAuthenticatorRequestInterceptor(HttpClientConfigurationHelper config,
			CasAuthenticator casAuthenticator) {
		return new CasAuthenticationHttpRequestInterceptor(config, casAuthenticator);
	}
}
