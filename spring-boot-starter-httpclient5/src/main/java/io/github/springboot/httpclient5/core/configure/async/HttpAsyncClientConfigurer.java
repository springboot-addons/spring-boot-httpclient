package io.github.springboot.httpclient5.core.configure.async;

import io.github.springboot.httpclient5.core.configure.CustomHttpRoutePlanner;
import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.async.AsyncExecChainHandler;
import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.core5.http.ConnectionReuseStrategy;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.configure.ConfigurableHttpRequestRetryStrategy;
import io.github.springboot.httpclient5.core.utils.ThreadFactoryUtils;

@Configuration
public class HttpAsyncClientConfigurer {

	@Autowired(required = false)
	private ObjectProvider<HttpAsyncClientBuilder> httpAsyncClientBuilderProvider;

	@Autowired(required = false)
	private ObjectProvider<CookieStore> cookieStoreProvider;

	@Autowired
	private ObjectProvider<HttpRequestInterceptor> requestInterceptors;

	@Autowired
	private ObjectProvider<AsyncExecChainHandler> execChainHandlerProvider;

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
	
	@Bean(destroyMethod = "initiateShutdown")
	public CloseableHttpAsyncClient closeableHttpAsyncClient(PoolingAsyncClientConnectionManager cm) {
		HttpAsyncClientBuilder builder = httpAsyncClientBuilderProvider.getIfAvailable(HttpAsyncClientBuilder::create) ;
		
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
		builder.setIOReactorConfig(config.getIoReactor().build()) ;
		builder.setThreadFactory(ThreadFactoryUtils.getThreadFactory()) ;
		builder.setRoutePlanner(new CustomHttpRoutePlanner(config));
		CloseableHttpAsyncClient asyncClient = builder.build();
		asyncClient.start();
		return asyncClient;
	}
	
}