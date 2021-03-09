package io.github.springboot.httpclient.auth.cifs.autoconfigure;

import org.apache.http.client.config.AuthSchemes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.springboot.httpclient.auth.cifs.JCIFSNTLMSchemeFactory;
import io.github.springboot.httpclient.internal.NamedAuthSchemeProvider;

@Configuration
public class NtlmAuthenticationAutoConfiguration {

	@Bean
	@ConditionalOnProperty(name = "httpclient.core.auth.ntlm.enabled", havingValue = "true", matchIfMissing = true)
	public NamedAuthSchemeProvider jCifsNtlmSchemeFactory() {
		return new NamedAuthSchemeProvider(AuthSchemes.NTLM, new JCIFSNTLMSchemeFactory());
	}
}
