package io.github.springboot.httpclient5.core.configure;

import java.util.Date;
import java.util.List;

import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DefaultCookieStoresProvider {
	@Bean
	@ConditionalOnProperty(name = "httpclient.core.cookie-store.type", havingValue = "thread-local", matchIfMissing = false)
	public CookieStore threadLocalCookieStore() {
		return new ThreadLocalCookieStore();
	}

	@Bean
	@ConditionalOnProperty(name = "httpclient.core.cookie-store.type", havingValue = "shared", matchIfMissing = true)
	public CookieStore sharedCookieStore() {
		return new BasicCookieStore();
	}

	public static class ThreadLocalCookieStore implements CookieStore {

		private final ThreadLocal<BasicCookieStore> cookieStores = ThreadLocal
				.withInitial(() -> new BasicCookieStore());

		public ThreadLocalCookieStore() {
		}

		@Override
		public void addCookie(Cookie cookie) {
			cookieStores.get().addCookie(cookie);
		}

		@Override
		public List<Cookie> getCookies() {
			return cookieStores.get().getCookies();
		}

		@Override
		public boolean clearExpired(Date date) {
			return cookieStores.get().clearExpired(date);
		}

		@Override
		public void clear() {
			cookieStores.get().clear();
		}
	}
}
