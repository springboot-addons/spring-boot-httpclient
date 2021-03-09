package io.github.springboot.httpclient.core.internal;

import java.util.Date;
import java.util.List;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;

public class ThreadLocalCookieStore implements CookieStore {

	private final ThreadLocal<BasicCookieStore> cookieStores = ThreadLocal.withInitial(() -> new BasicCookieStore());

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
