package io.github.springboot.httpclient5.core.interceptors;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hc.client5.http.async.AsyncExecCallback;
import org.apache.hc.client5.http.async.AsyncExecChain;
import org.apache.hc.client5.http.async.AsyncExecChainHandler;
import org.apache.hc.client5.http.classic.ExecChain;
import org.apache.hc.client5.http.classic.ExecChain.Scope;
import org.apache.hc.client5.http.classic.ExecChainHandler;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class RequestConfigExecChainHandler implements ExecChainHandler, AsyncExecChainHandler {

	private final HttpClient5Config config;

	@Override
	public ClassicHttpResponse execute(ClassicHttpRequest request, Scope scope, ExecChain chain)
			throws IOException, HttpException {
		try {
			configure(request, scope.clientContext) ;
			return chain.proceed(request, scope);
		
		} catch (HttpException | IOException e) {
			log.warn("Unable to configure httpclient request, no uri available : using defaut configuration", e);
			throw e ;
		}
	}

	@Override
	public void execute(HttpRequest request, AsyncEntityProducer entityProducer,
			org.apache.hc.client5.http.async.AsyncExecChain.Scope scope, AsyncExecChain chain,
			AsyncExecCallback asyncExecCallback) throws HttpException, IOException {
		configure(request, scope.clientContext) ;
		chain.proceed(request, entityProducer, scope, asyncExecCallback) ;
	}

	public void configure(HttpRequest request, HttpClientContext context)
			throws HttpException {
		try {
			URI uri = request.getUri() ;
			final String method = request.getMethod();
			log.debug("Configuring httpclient for {} {}", method, uri);
			
			// connection-request-timeout management, HttpRequestConfigurerInterceptor is too late in the process
			RequestConfigProperties requestConfigProperties = config.getRequestConfigProperties(method, uri.toString());
			RequestConfig requestConfig = requestConfigProperties.build();
			// SRU sb 3.3 : to be removed
			scope.clientContext.setAttribute(HttpClientContext.REQUEST_CONFIG, requestConfig);
			// SRU sb 3.4 : to be keept
			HttpClientContext.castOrCreate(scope.clientContext).setRequestConfig(requestConfig); ;
			
			return chain.proceed(request, scope);
		
		} catch (HttpException | IOException e) {
			log.warn("Unable to configure httpclient request, no uri available : using defaut configuration", e);
			throw e ;
		} catch (URISyntaxException e) {
			throw new HttpException(e.getMessage()) ;
		}
	}

}