package io.github.springboot.httpclient5.core.configure;

import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.client5.http.classic.ExecChainHandler;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.fluent.Executor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ConnectionReuseStrategy;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;

@Configuration
public class HttpClientConfigurer {

	@Autowired(required = false)
	private ObjectProvider<CookieStore> cookieStoreProvider;

	@Autowired
	private ObjectProvider<HttpRequestInterceptor> requestInterceptors;

	@Autowired
	private ObjectProvider<ExecChainHandler> execChainHandlerProvider;

	
	@Autowired
	private ObjectProvider<HttpResponseInterceptor> responseInterceptors;

	@Autowired
	private ObjectProvider<CredentialsStore> credentialsStoreProvider ;

	@Autowired
	private ObjectProvider<HttpRequestRetryStrategy> httpRequestRetryStrategyProvider ;

	@Autowired
	private ObjectProvider<ConnectionReuseStrategy> connectionReuseStrategyProvider ;

	@Autowired
	private ObjectProvider<ConnectionKeepAliveStrategy> connectionKeepAliveStrategyProvider ;

	
	@Autowired
	private HttpClient5Config config ;
	
	@Bean
	public Executor httpClientExecutor(CloseableHttpClient httpClient) {
		return Executor.newInstance(httpClient);
	}	
	
	@Bean
	public CloseableHttpClient closeableHttpClient(PoolingHttpClientConnectionManager cm) {
		HttpClientBuilder builder = HttpClientBuilder.create() ;
		builder.setUserAgent(config.getUserAgent()) ;
		builder.setConnectionManager(cm) ;
	    requestInterceptors.orderedStream().forEach(builder::addRequestInterceptorLast);
	    responseInterceptors.orderedStream().forEach(builder::addResponseInterceptorLast);
	    execChainHandlerProvider.orderedStream().forEach(e -> builder.addExecInterceptorFirst(e.getClass().getName(), e));
	    
		if (cookieStoreProvider.getIfAvailable() == null) {
			builder.disableCookieManagement() ;
		}
		cookieStoreProvider.ifAvailable(builder::setDefaultCookieStore);
		credentialsStoreProvider.ifAvailable(builder::setDefaultCredentialsProvider);
		connectionKeepAliveStrategyProvider.ifAvailable(builder::setKeepAliveStrategy);		
		connectionReuseStrategyProvider.ifAvailable(builder::setConnectionReuseStrategy);		

		HttpRequestRetryStrategy retryStrategy = httpRequestRetryStrategyProvider.getIfAvailable() ;
		if (retryStrategy == null) {
			retryStrategy = new ConfigurableHttpRequestRetryStrategy(config) ;
		}
		builder.setRetryStrategy(retryStrategy) ;
		
		return builder.build();
	}
	
}