package io.github.springboot.httpclient5.core.configure;

import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.client5.http.classic.ExecChainHandler;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.fluent.Executor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

	
	@Bean
	public Executor httpClientExecutor(CloseableHttpClient httpClient) {
		return Executor.newInstance(httpClient);
	}	
	
	@Bean
	public CloseableHttpClient closeableHttpClient(PoolingHttpClientConnectionManager cm) {
		HttpClientBuilder builder = HttpClientBuilder.create() ;
		builder.setConnectionManager(cm) ;
	    requestInterceptors.forEach(builder::addRequestInterceptorLast);
	    responseInterceptors.forEach(builder::addResponseInterceptorLast);
	    execChainHandlerProvider.orderedStream().forEach(e -> builder.addExecInterceptorFirst(e.getClass().getName(), e));
	    
		if (cookieStoreProvider.getIfAvailable() == null) {
			builder.disableCookieManagement() ;
		}
		cookieStoreProvider.ifAvailable(builder::setDefaultCookieStore);
		credentialsStoreProvider.ifAvailable(builder::setDefaultCredentialsProvider);
		
		return builder.build();
	}
	
}