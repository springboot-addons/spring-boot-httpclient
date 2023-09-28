package io.github.springboot.httpclient.web.headers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicHeader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.context.WebApplicationContext;

@Configuration
public class RequestHeadersProviders {

	public interface RequestHeadersStorage {
		void add(Header h);

		void add(String key, String value);

		void add(String key, Enumeration<String> values);

		List<Header> getHeaderList();

		void clear();
	}

	public static class ThreadLocalRequestHeaderStorage implements RequestHeadersStorage {
		private static final ThreadLocal<RequestHeadersStorage> storages = ThreadLocal
				.withInitial(DefaultRequestHeadersStorage::new);

		@Override
		public void add(Header h) {
			storages.get().add(h);
		}

		@Override
		public void add(String key, String value) {
			storages.get().add(key, value);
		}

		@Override
		public void add(String key, Enumeration<String> values) {
			storages.get().add(key, values);
		}

		@Override
		public List<Header> getHeaderList() {
			return storages.get().getHeaderList();
		}

		@Override
		public void clear() {
			storages.get().clear();
		}
	}

	public static class DefaultRequestHeadersStorage implements RequestHeadersStorage {
		private final Map<String, Set<String>> internal = new ConcurrentHashMap<>();

		@Override
		public void add(Header h) {
			add(h.getName(), h.getValue());
		}

		@Override
		public void add(String key, String value) {
			Set<String> values = internal.computeIfAbsent(key, k -> new ConcurrentSkipListSet<>());
			values.add(value);
		}

		@Override
		public void add(String key, Enumeration<String> values) {
			Collections.list(values).forEach(val -> add(key, val));
		}

		@Override
		public List<Header> getHeaderList() {
			List<Header> headers = new ArrayList<>();
			for (Entry<String, Set<String>> entry : internal.entrySet()) {
				for (String value : entry.getValue()) {
					headers.add(new BasicHeader(entry.getKey(), value));
				}
			}
			return headers;
		}

		@Override
		public void clear() {
			internal.clear();
		}
	}

	@Bean("downHeaders")
	@ConditionalOnWebApplication
	@Scope(scopeName = WebApplicationContext.SCOPE_REQUEST)
	public RequestHeadersStorage downHeadersWeb() {
		return new DefaultRequestHeadersStorage();
	}

	@Bean("upHeaders")
	@ConditionalOnWebApplication
	@Scope(scopeName = WebApplicationContext.SCOPE_REQUEST)
	public RequestHeadersStorage upHeadersWeb() {
		return new DefaultRequestHeadersStorage();
	}

	@Bean("downHeaders")
	@ConditionalOnNotWebApplication
	public RequestHeadersStorage downHeaders() {
		return new ThreadLocalRequestHeaderStorage();
	}

	@Bean("upHeaders")
	@ConditionalOnNotWebApplication
	public RequestHeadersStorage upHeaders() {
		return new ThreadLocalRequestHeaderStorage();
	}

}