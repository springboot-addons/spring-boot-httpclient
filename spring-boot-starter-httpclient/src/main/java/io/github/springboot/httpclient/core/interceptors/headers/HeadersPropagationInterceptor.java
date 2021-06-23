package io.github.springboot.httpclient.core.interceptors.headers;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.github.springboot.httpclient.core.interceptors.headers.RequestHeadersProviders.RequestHeadersStorage;
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
	private HeadersPropagationConfig config;

	@Override
	public void process(HttpResponse response, HttpContext context) {

		if (config.isEnabledPropagation()) {
			try {
				for (Header h : response.getAllHeaders()) {
					if (config.getUpPattern().matcher(h.getName()).matches()) {
						log.debug("*** Storing header {} from subsequent call", h);
						upHeadersProvider.ifAvailable(
								requestHeadersStorage -> requestHeadersStorage.add(h.getName(), h.getValue()));
					}
				}
			} catch (Exception e) {
				// ignore
			}
		}
	}

	@Override
	public void process(HttpRequest request, HttpContext context) {

		if (config.isEnabledPropagation()) {
			try {
				downHeadersProvider.ifAvailable(requestHeadersStorage -> requestHeadersStorage.getHeaderList().stream()
						.filter(h -> config.getDownPattern().matcher(h.getName()).matches()).forEach(h -> {
							log.debug("*** Using header {} from storage for subsequent call to {}", h,
									request.getRequestLine());
							request.addHeader(h);
						}));
			} catch (Exception e) {
				// ignore
			}
		}
	}

}
