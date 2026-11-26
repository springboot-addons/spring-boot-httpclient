package io.github.springboot.httpclient5.core.config.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.junit.jupiter.api.Test;

class SimplePredefinedCredentialsProviderTest {

	@Test
	void shouldParseBasicCredentials() {
		SimplePredefinedCredentialsProvider provider = new SimplePredefinedCredentialsProvider("admin:pwd");

		UsernamePasswordCredentials credentials = (UsernamePasswordCredentials) provider.getCredentials(null, null);

		assertEquals("admin", credentials.getUserName());
		assertEquals("pwd", new String(credentials.getPassword()));
		assertFalse(provider.isPreemptive());
	}

	@Test
	void shouldParsePrefixedCredentials() {
		SimplePredefinedCredentialsProvider provider = new SimplePredefinedCredentialsProvider("PREEMPTIVE(admin:pwd)");

		assertTrue(provider.isPreemptive());
		assertEquals("admin", provider.getCredentials(null, null).getUserPrincipal().getName());
		assertEquals(Base64.getEncoder().encodeToString("admin:pwd".getBytes(StandardCharsets.UTF_8)),
				provider.toBase64Encoded());
	}

	@Test
	void shouldParseBasicPrefixWithoutBeingPreemptive() {
		SimplePredefinedCredentialsProvider provider = new SimplePredefinedCredentialsProvider("BASIC(admin:pwd)");

		assertFalse(provider.isPreemptive());
		assertEquals("admin", provider.getCredentials(null, null).getUserPrincipal().getName());
		assertEquals("pwd", new String(provider.getCredentials(null, null).getPassword()));
	}

	@Test
	void shouldKeepColonsInPassword() {
		SimplePredefinedCredentialsProvider provider = new SimplePredefinedCredentialsProvider("admin:p:a:ss");

		assertEquals("admin", provider.getCredentials(null, null).getUserPrincipal().getName());
		assertEquals("p:a:ss", new String(provider.getCredentials(null, null).getPassword()));
	}

	@Test
	void shouldTrimValues() {
		SimplePredefinedCredentialsProvider provider = new SimplePredefinedCredentialsProvider("  admin:pwd  ");

		assertEquals("admin", provider.getCredentials(null, null).getUserPrincipal().getName());
	}

	@Test
	void shouldRejectInvalidCredentials() {
		SimplePredefinedCredentialsProvider provider = new SimplePredefinedCredentialsProvider("admin");

		assertThrows(IllegalArgumentException.class, () -> provider.getCredentials(null, null));
	}

	@Test
	void shouldRejectBlankUserName() {
		SimplePredefinedCredentialsProvider provider = new SimplePredefinedCredentialsProvider(":pwd");

		assertThrows(IllegalArgumentException.class, () -> provider.getCredentials(null, null));
	}

	@Test
	void shouldRejectUnclosedPrefix() {
		SimplePredefinedCredentialsProvider provider = new SimplePredefinedCredentialsProvider("PREEMPTIVE(admin:pwd");

		assertThrows(IllegalArgumentException.class, () -> provider.getCredentials(null, null));
	}

	@Test
	void shouldRejectNullValue() {
		assertThrows(NullPointerException.class, () -> new SimplePredefinedCredentialsProvider(null));
	}
}
