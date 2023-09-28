package io.github.springboot.httpclient5.core.config.model;

import java.util.Arrays;
import java.util.Base64;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.core5.http.protocol.HttpContext;

import lombok.Data;

@Data
public class SimplePredefinedCredentialsProvider implements CredentialsProvider {
	
	private static final String PREEMPTIVE_PREFIX = "PREEMPTIVE(";
	private static final String BASIC_PREFIX = "BASIC(";
	private String credentialAsString;
	private boolean preemptive ;

	public SimplePredefinedCredentialsProvider(String credentialAsString) {
		this.credentialAsString = credentialAsString;
		this.preemptive = credentialAsString.startsWith(PREEMPTIVE_PREFIX) ;
	}
	
	public String toBase64Encoded() {
		return Base64.getEncoder().encodeToString(removePrefixes(credentialAsString).getBytes());
	}
	
	@Override
	public Credentials getCredentials(AuthScope authScope, HttpContext context) {
		// TODO manage prefix and switch case for KerberosCredentials
		String[] parts = removePrefixes(credentialAsString).split(":") ;
		return new UsernamePasswordCredentials(parts[0], parts[1].toCharArray());
	}

	private String removePrefixes(String s) {
		StringBuilder c = new StringBuilder(s.trim());
		Arrays.asList(BASIC_PREFIX, PREEMPTIVE_PREFIX).forEach(prefix -> {
			if (c.indexOf(prefix) == 0) {
				c.delete(0, prefix.length()) ;
				c.delete(c.length() -1, c.length()) ;
			}
		});
		return c.toString();
	}

}
