package io.github.springboot.httpclient5.core.interceptors;

import java.io.IOException;
import java.net.URI;

import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;
import io.github.springboot.httpclient5.core.config.model.SimplePredefinedCredentialsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class HttpRequestConfigurerInterceptor implements HttpRequestInterceptor {

	static final String REQUEST_CONFIG_EXTENDED = HttpClientContext.REQUEST_CONFIG+"-extended";
	
	private final HttpClient5Config config;

	@Override
	public void process(HttpRequest request, EntityDetails entity, HttpContext context)
			throws HttpException, IOException {
		try {
			URI uri = request.getUri() ;
			final String method = request.getMethod();
			log.debug("Configuring httpclient for {} {}", method, uri);
			
			RequestConfigProperties requestConfigProperties = config.getRequestConfigProperties(method, uri.toString());
			context.setAttribute(HttpClientContext.REQUEST_CONFIG, requestConfigProperties.build());
			context.setAttribute(REQUEST_CONFIG_EXTENDED, requestConfigProperties);
			
			SimplePredefinedCredentialsProvider credentials = requestConfigProperties.getCredentials() ;
			if (credentials != null && credentials.isPreemptive()) {
				request.addHeader(new BasicHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials.toBase64Encoded()));
			}
			else {
				context.setAttribute(HttpClientContext.CREDS_PROVIDER, requestConfigProperties.getCredentials());
			}
			
			requestConfigProperties.getCustomRequestContext().forEach(context::setAttribute);
		
		} catch (final Exception e) {
			log.warn("Unable to configure httpclient request, no uri available : using defaut configuration", e);
		}
	}
	
}