package io.github.springboot.httpclient5.core.interceptors;

import java.io.IOException;

import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
@Component("hc5httpRequestConfigurerInterceptorPostCleanup")
public class HttpRequestConfigurerInterceptorPostCleanup implements HttpResponseInterceptor {

	@Override
	public void process(HttpResponse response, EntityDetails entity, HttpContext context)
			throws HttpException, IOException {
		RequestConfigProperties requestConfigProperties = (RequestConfigProperties) context.removeAttribute(HttpRequestConfigurerInterceptor.REQUEST_CONFIG_EXTENDED);
		if (requestConfigProperties != null) {
			requestConfigProperties.getCustomRequestContext().keySet().forEach(context::removeAttribute) ;
		}
	}
}