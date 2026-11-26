package io.github.springboot.httpclient5.core.config.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.format.DateTimeParseException;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.impl.InMemoryDnsResolver;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Test;

import io.github.springboot.httpclient5.core.config.model.SimplePredefinedCredentialsProvider;

class ConvertersTest {

	private final TimeoutConverter timeoutConverter = new TimeoutConverter();
	private final IntegerTimeoutConverter integerTimeoutConverter = new IntegerTimeoutConverter();
	private final StringTimeValueConverter stringTimeValueConverter = new StringTimeValueConverter();
	private final IntegerTimeValueConverter integerTimeValueConverter = new IntegerTimeValueConverter();
	private final HttpHostConverter httpHostConverter = new HttpHostConverter();
	private final DnsResolverConverter dnsResolverConverter = new DnsResolverConverter();
	private final StringCredentialsProviderConverter credentialsProviderConverter = new StringCredentialsProviderConverter();

	@Test
	void shouldConvertDurationToTimeout() {
		Timeout timeout = timeoutConverter.convert("PT0.5S");

		assertEquals(500, timeout.convert(TimeUnit.MILLISECONDS));
	}

	@Test
	void shouldRejectInvalidDuration() {
		assertThrows(DateTimeParseException.class, () -> timeoutConverter.convert("500ms"));
	}

	@Test
	void shouldConvertIntegerToTimeoutInMilliseconds() {
		assertEquals(1500, integerTimeoutConverter.convert(1500).toMilliseconds());
	}

	@Test
	void shouldConvertDurationToTimeValue() {
		TimeValue value = stringTimeValueConverter.convert("PT3S");

		assertEquals(3000, value.toMilliseconds());
	}

	@Test
	void shouldConvertIntegerToTimeValueInMilliseconds() {
		assertEquals(2500, integerTimeValueConverter.convert(2500).toMilliseconds());
	}

	@Test
	void shouldConvertHttpHost() {
		HttpHost host = httpHostConverter.convert("https://example.com:8443");

		assertEquals("https", host.getSchemeName());
		assertEquals("example.com", host.getHostName());
		assertEquals(8443, host.getPort());
	}

	@Test
	void shouldResolveSystemDnsResolverCaseInsensitively() {
		assertInstanceOf(SystemDefaultDnsResolver.class, dnsResolverConverter.convert("system"));
	}

	@Test
	void shouldResolveInMemoryDnsResolver() {
		assertInstanceOf(InMemoryDnsResolver.class, dnsResolverConverter.convert("IN_MEMORY"));
	}

	@Test
	void shouldInstantiateCustomDnsResolver() {
		assertInstanceOf(InMemoryDnsResolver.class,
				dnsResolverConverter.convert(InMemoryDnsResolver.class.getName()));
	}

	@Test
	void shouldFailOnUnknownDnsResolverClass() {
		assertThrows(ClassNotFoundException.class, () -> dnsResolverConverter.convert("com.example.UnknownResolver"));
	}

	@Test
	void shouldConvertCredentials() {
		SimplePredefinedCredentialsProvider credentials = (SimplePredefinedCredentialsProvider) credentialsProviderConverter
				.convert("admin:pwd");

		assertEquals("admin:pwd", credentials.getCredentialAsString());
		assertTrue(!credentials.isPreemptive());
	}
}
