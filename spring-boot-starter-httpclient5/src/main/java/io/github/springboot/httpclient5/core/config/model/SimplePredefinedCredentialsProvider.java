package io.github.springboot.httpclient5.core.config.model;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.core5.http.protocol.HttpContext;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SimplePredefinedCredentialsProvider implements CredentialsProvider {
	
	private static final String PREEMPTIVE_PREFIX = "PREEMPTIVE(";
	private static final String BASIC_PREFIX = "BASIC(";
	private static final List<String> PREFIXES = Arrays.asList(BASIC_PREFIX, PREEMPTIVE_PREFIX);

	private String credentialAsString;
	private boolean preemptive ;

	public SimplePredefinedCredentialsProvider(String credentialAsString) {
		this.credentialAsString = Objects.requireNonNull(credentialAsString, "credentials must not be null");
		this.preemptive = this.credentialAsString.trim().startsWith(PREEMPTIVE_PREFIX) ;
	}
	
	public String toBase64Encoded() {
		return Base64.getEncoder().encodeToString(removePrefixes(credentialAsString).getBytes(StandardCharsets.UTF_8));
	}
	
	@Override
	public Credentials getCredentials(AuthScope authScope, HttpContext context) {
		// TODO manage prefix and switch case for KerberosCredentials
		String[] parts = removePrefixes(credentialAsString).split(":", 2) ;
		if (parts.length != 2 || parts[0].isBlank()) {
			throw new IllegalArgumentException("Credentials must use the 'user:password' format");
		}
		return new UsernamePasswordCredentials(parts[0], parts[1].toCharArray());
	}

	private String removePrefixes(String s) {
		String value = s.trim();
		for (String prefix : PREFIXES) {
			if (value.startsWith(prefix)) {
				if (!value.endsWith(")")) {
					throw new IllegalArgumentException("Credentials with prefix '" + prefix + "' must end with ')'");
				}
				return value.substring(prefix.length(), value.length() - 1);
			}
		}
		return value;
	}

}
