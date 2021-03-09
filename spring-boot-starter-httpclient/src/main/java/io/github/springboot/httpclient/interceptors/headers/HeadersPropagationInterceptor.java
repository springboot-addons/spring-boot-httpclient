package io.github.springboot.httpclient.interceptors.headers;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.util.ContentCachingResponseWrapper;

import io.github.springboot.httpclient.interceptors.headers.RequestHeadersProviders.RequestHeadersStorage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HeadersPropagationInterceptor implements Filter, HttpRequestInterceptor, HttpResponseInterceptor {

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

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		if (request instanceof HttpServletRequest) {

			HttpServletRequest hRequest = (HttpServletRequest) request;
			Enumeration<String> headerNames = hRequest.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String headerName = headerNames.nextElement();
				if (config.getDownPattern().matcher(headerName).matches()) {
					log.debug("*** Storing incomming header {} : {}", headerName, hRequest.getHeaders(headerName));
					downHeadersProvider.ifAvailable(requestHeadersStorage -> requestHeadersStorage.add(headerName,
							hRequest.getHeaders(headerName)));
				}
			}

			HttpServletResponse hResponse = (HttpServletResponse) response;
			ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(hResponse);
			chain.doFilter(hRequest, responseWrapper);

			upHeadersProvider.ifAvailable(requestHeadersStorage -> requestHeadersStorage.getHeaderList().stream()
					.filter(eh -> config.getUpPattern().matcher(eh.getName()).matches()).forEach(eh -> {
						log.debug("*** Forwarding header from storage {}", eh);
						responseWrapper.addHeader(eh.getName(), eh.getValue());
					}));

			responseWrapper.copyBodyToResponse();

		} else {
			chain.doFilter(request, response);
		}
	}

}
