package io.github.springboot.httpclient5;

import java.io.IOException;

import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.github.springboot.httpclient5.core.interceptors.ActivableHttpInterceptor;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(10)
@Slf4j
public class TestInterceptor extends ActivableHttpInterceptor{
	public static boolean wasActivated = false ;
	
	public TestInterceptor() {
		super("tu-interceptor");
	}

	@Override
	public void doProcess(HttpResponse response, EntityDetails entity, HttpContext context)
			throws HttpException, IOException {
		
	}

	@Override
	public void doProcess(HttpRequest request, EntityDetails entity, HttpContext context)
			throws HttpException, IOException {
		wasActivated = true ;
		log.info("Test Interceptor called");
	}

}
