package io.github.springboot.httpclient.internal;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class CookieStoreProvider {
	@Bean
	@ConditionalOnProperty(name = "httpclient.core.cookie-store.type", havingValue = "thread-local", matchIfMissing = false)
	public CookieStore threadLocalCookieStore() {
		return new ThreadLocalCookieStore();
	}

	@Bean
	@ConditionalOnProperty(name = "httpclient.core.cookie-store.type", havingValue = "request", matchIfMissing = false)
	@RequestScope(proxyMode = ScopedProxyMode.INTERFACES)
	public CookieStore requestCookieStore() {
		return new BasicCookieStore();
	}

	@Bean
	@ConditionalOnProperty(name = "httpclient.core.cookie-store.type", havingValue = "shared", matchIfMissing = true)
	public CookieStore sharedCookieStore() {
		return new BasicCookieStore();
	}
}
