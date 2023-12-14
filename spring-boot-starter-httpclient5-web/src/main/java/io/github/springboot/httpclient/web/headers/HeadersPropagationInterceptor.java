package io.github.springboot.httpclient.web.headers;

import java.io.IOException;

import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.github.springboot.httpclient.web.headers.RequestHeadersProviders.RequestHeadersStorage;
import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.HeadersPropagationProperties;
import io.github.springboot.httpclient5.core.utils.PatternUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HeadersPropagationInterceptor implements HttpRequestInterceptor, HttpResponseInterceptor {

	@Autowired
	@Qualifier("downHeaders")
	private ObjectProvider<RequestHeadersStorage> downHeadersProvider;

	@Autowired
	@Qualifier("upHeaders")
	private ObjectProvider<RequestHeadersStorage> upHeadersProvider;

	@Autowired
	private HttpClient5Config config;

	@Override
    public void process(HttpResponse response, EntityDetails entity, HttpContext context) throws HttpException, IOException {

		HeadersPropagationProperties config = getConfiguration(context) ;
		
		RequestHeadersStorage requestHeadersStorage = upHeadersProvider.getIfAvailable() ; 
		if (config.getEnabled() != null && config.getEnabled() && requestHeadersStorage != null) {
			try {
				for (Header h : response.getHeaders()) {
					if (PatternUtils.matchesOne(h.getName(), config.getUp())) {
						log.debug("*** Storing header {} from subsequent call", h);
						requestHeadersStorage.add(h.getName(), h.getValue());
					}
				}
			} catch (Exception e) {
				// ignore
			}
		}
	}

	@Override
	public void process(HttpRequest request, EntityDetails entity, HttpContext context) throws HttpException, IOException {
		HeadersPropagationProperties config = getConfiguration(context) ;
		
		if (config.getEnabled() != null && config.getEnabled() && downHeadersProvider != null) {
			try {
				downHeadersProvider.ifAvailable(requestHeadersStorage -> requestHeadersStorage.getHeaderList().stream()
						.filter(h -> PatternUtils.matchesOne(h.getName(), config.getDown()))
						.forEach(h -> {
							if (log.isDebugEnabled()) {
								log.debug("*** Using header {} from storage for subsequent call to {}", h,
										request.getRequestUri());
							}
							request.addHeader(h);
						}));
			} catch (Exception e) {
				// ignore
			}
			config.getAdd().forEach(request::addHeader);
			config.getRemove().forEach(request::removeHeaders);
		}
	}
	
	@SneakyThrows
	protected HeadersPropagationProperties getConfiguration(HttpContext httpContext) {
		final HttpClientContext clientContext = HttpClientContext.adapt(httpContext);
		HttpRequest request = (HttpRequest) clientContext.getAttribute(HttpClientContext.HTTP_REQUEST);
		return config.getRequestConfigProperties(request.getMethod(), request.getUri().toString()).getHeadersPropagation();
	}
}
