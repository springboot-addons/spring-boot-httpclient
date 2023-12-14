package io.github.springboot.httpclient5.core.interceptors;

import java.io.IOException;

import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class ActivableHttpInterceptor implements HttpRequestInterceptor, HttpResponseInterceptor {
	private final String id;
	private final boolean activateByDefault;
	
	public ActivableHttpInterceptor(String id) {
		this(id, false) ;
	}
	
	@Override
	public final void process(HttpResponse response, EntityDetails entity, HttpContext context)
			throws HttpException, IOException {
		if (isActivated(context)) {
			doProcess(response, entity, context);
		}
	}

	public abstract void doProcess(HttpResponse response, EntityDetails entity, HttpContext context)
			throws HttpException, IOException ;


	@Override
	public final void process(HttpRequest request, EntityDetails entity, HttpContext context)
			throws HttpException, IOException {
		if (isActivated(context)) {
			doProcess(request, entity, context);
		}
	}

	public abstract void doProcess(HttpRequest request, EntityDetails entity, HttpContext context)
			throws HttpException, IOException ; 
	
	protected boolean isActivated(HttpContext context) {
		RequestConfigProperties c = (RequestConfigProperties) context.getAttribute(HttpRequestConfigurerInterceptor.REQUEST_CONFIG_EXTENDED) ;
		Boolean b = c.getInterceptors().get(id);
		return (b != null && b) || (b == null && activateByDefault) ;
	}

}
