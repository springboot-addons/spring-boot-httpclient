package io.github.springboot.httpclient5;

import org.apache.hc.client5.http.fluent.Executor;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.HttpResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import lombok.extern.slf4j.Slf4j;

/**
 * http client auto configuration tests
 *
 * @author linux_china
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
@Slf4j
public class FluentTests {

	@Autowired
	Executor executor ; 

	@Test
	public void testExecutor() throws Exception {
		final HttpResponse response = executor.execute(Request.get("https://httpbin.agglo-larochelle.fr/headers")).returnResponse();
		Assertions.assertEquals(200, response.getCode()) ;
	}
		
	
}
