package io.github.springboot.httpclient5.core.utils;

import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.core5.concurrent.FutureCallback;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingFutureCallback implements FutureCallback<SimpleHttpResponse> {
	public static LoggingFutureCallback INSTANCE = new LoggingFutureCallback() ;
	
	@Override
	public void failed(Exception ex) {
		log.warn("Failed async http request : {}", ex.getMessage());
	}

	@Override
	public void completed(SimpleHttpResponse result) {
		log.debug("completed async http request, code={}", result.getCode());
	}

	@Override
	public void cancelled() {
		log.info("cancelled async http request");
	}
}
