package io.github.springboot.httpclient5.core.configure;

import java.net.URI;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.SchemePortResolver;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.stereotype.Component;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MaxPerRouteConnectionManagerConfigurer implements PoolingHttpClientConnectionManagerPostConfigurer {
	private final HttpClient5Config config; 
	private final SchemePortResolver schemePortResolver; 
	
	@Override
	public void configure(PoolingHttpClientConnectionManager cm) {
		config.getPool().getHostConfig().entrySet().stream().forEach(e -> {
			String url = e.getKey() ;
			if (!url.endsWith("/")) {
				url = url + "/";
			}
			HttpHost proxy = config.getRequestConfig("GET", url).getProxy() ;
			final HttpRoute httpRoute = getHttpRoute(url, proxy);
			
			Integer maxActive = e.getValue() ;
			if (maxActive != null) {
				log.info("Configuring maxPerRoute for {} via {} to {}", url, proxy, maxActive) ;
				cm.setMaxPerRoute(httpRoute, maxActive);
			}
		});
	}
	
	
	@SneakyThrows
	protected HttpRoute getHttpRoute(final String uri, HttpHost proxy) {
		HttpHost h = HttpHost.create(new URI(uri));
		HttpHost target = new HttpHost(h.getHostName(), schemePortResolver.resolve(h));
		return proxy == null ? new HttpRoute(target) : new HttpRoute(target, proxy);
	}	
}

