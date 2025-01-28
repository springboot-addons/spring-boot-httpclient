package io.github.springboot.httpclient5.core.configure;

import java.net.URI;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.SchemePortResolver;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.CommonsPoolProperties;
import io.github.springboot.httpclient5.core.config.model.ConnectionConfigProperties;
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
	public void configure(ConfigurableConnPoolControl cm, boolean asyncPool) {
		CommonsPoolProperties poolProperties = asyncPool ?  config.getAsyncPool() : config.getPool() ;
		poolProperties.getHostConfig().entrySet().stream().forEach(e -> {
			String url = e.getKey() ;
			if (!url.endsWith("/")) {
				url = url + "/";
			}
			HttpHost proxy = config.getRequestConfig("GET", url).getProxy() ;
			final HttpRoute httpRoute = getHttpRoute(url, proxy);
			
			ConnectionConfigProperties connProperties = e.getValue() ;
			
			Integer maxConnections = connProperties.getMaxConnections() ;
			if (maxConnections != null) {
				log.info("Configuring maxPerRoute for {} via {} to {}", url, proxy,maxConnections) ;
				cm.setMaxPerRoute(httpRoute, maxConnections);
			}
			
			cm.setConnectionConfigResolver(route -> getConnectionConfig(route, poolProperties)) ;
		});
	}
	
	protected ConnectionConfig getConnectionConfig(HttpRoute route, CommonsPoolProperties poolProperties) {
		String routeUri = route.getTargetHost().toURI() ;
		
		ConnectionConfigProperties connectionConfigProperties = poolProperties.getHostConfig().get(routeUri) ;
		if (connectionConfigProperties == null) {
			return null ;
		}
		
		if (connectionConfigProperties.getConnectTimeout().equals(Timeout.ofMinutes(3))) {
			connectionConfigProperties.setConnectTimeout(poolProperties.getDefaultConnectionConfig().getConnectTimeout()) ;
		}
		if (log.isDebugEnabled()) {
			log.debug("Using connectionConfig for {} => {}", routeUri, connectionConfigProperties) ;
		}	
		return connectionConfigProperties.build() ;
	}
	
	@SneakyThrows
	protected HttpRoute getHttpRoute(final String uri, HttpHost proxy) {
		HttpHost h = HttpHost.create(new URI(uri));
		boolean secure = "https".equals(h.getSchemeName()) ;
		HttpHost target = new HttpHost(h.getSchemeName(), h.getHostName(), schemePortResolver.resolve(h));
		return proxy == null ? new HttpRoute(target, null, secure) : new HttpRoute(target, null, proxy, secure);
	}	
}

