package io.github.springboot.httpclient.core.internal;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.protocol.HttpContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.springboot.httpclient.core.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.core.config.model.ProxyConfiguration;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class HttpProxyConfig {
	
	@Bean
	public HttpRoutePlanner configurableHttpRoutePlanner(HttpClientConfigurationHelper config) {
		return new DefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE) {
			@Override
			protected HttpHost determineProxy(HttpHost target, HttpRequest request, HttpContext context)
					throws HttpException {
				String uri = target.toURI() ;
				if (config.useProxyForHost(uri)) {
					ProxyConfiguration proxyConfiguration = config.getProxyConfiguration(uri) ;
					log.info("Using proxy {} for {}", proxyConfiguration, uri);
					return new HttpHost(proxyConfiguration.getHost(), proxyConfiguration.getPort()) ;
				}
				else {
					return null;
				}
			}
		} ;
	}

}
