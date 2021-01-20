package io.github.springboot.httpclient.internal;

import org.apache.http.client.config.AuthSchemes;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.auth.DigestSchemeFactory;
import org.apache.http.impl.auth.KerberosSchemeFactory;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DefaultAuthSchemesConfiguration {
	
	@Bean
	@ConditionalOnProperty(name = "httpclient.core.auth.basic.enabled", havingValue = "true", matchIfMissing = true)
	public NamedAuthSchemeProvider basicAuthScheme() {
		return new NamedAuthSchemeProvider(AuthSchemes.BASIC, new BasicSchemeFactory());
	}

	@Bean
	@ConditionalOnProperty(name = "httpclient.core.auth.digest.enabled", havingValue = "true", matchIfMissing = true)
	public NamedAuthSchemeProvider digestAuthScheme() {
		return new NamedAuthSchemeProvider(AuthSchemes.DIGEST, new DigestSchemeFactory());
	}

	@Bean
	@ConditionalOnProperty(name = "httpclient.core.auth.spnego.enabled", havingValue = "true", matchIfMissing = true)
	public NamedAuthSchemeProvider spnegoAuthScheme() {
		return new NamedAuthSchemeProvider(AuthSchemes.SPNEGO, new SPNegoSchemeFactory(true));
	}

	@Bean
	@ConditionalOnProperty(name = "httpclient.core.auth.kerberos.enabled", havingValue = "true", matchIfMissing = true)
	public NamedAuthSchemeProvider kerberosAuthScheme() {
		return new NamedAuthSchemeProvider(AuthSchemes.KERBEROS, new KerberosSchemeFactory(true));
	}
}
