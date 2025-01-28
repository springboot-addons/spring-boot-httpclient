package io.github.springboot.httpclient5;
import java.util.concurrent.Future;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpStreamResetException;
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
 * @author sru
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE, 
		properties = { "spring.httpclient5.user-agent=HttpClient5Async/SRU",
			    "spring.httpclient5.request-config.default.retry-config.max-retries=0"})
@ActiveProfiles("test")
@Slf4j
public class AsyncTests {

	@Autowired
	CloseableHttpAsyncClient async;

	@Test
	public void testAsyncHttpClient() throws Exception {
		final SimpleHttpRequest httpGet = SimpleHttpRequest.create("GET", Constants.HTTPBIN_TEST_HOST + "/headers");
		Future<SimpleHttpResponse> future = async.execute(httpGet, new FutureCallback<SimpleHttpResponse>() {

			@Override
			public void completed(SimpleHttpResponse result) {
				System.out.println("Completed");
			}

			@Override
			public void failed(Exception ex) {
				ex.printStackTrace();
				Assertions.fail(ex) ;
			}

			@Override
			public void cancelled() {
				Assertions.fail("Should not have been cancel") ;
			}
		});
		
		SimpleHttpResponse response = future.get() ;
		Assertions.assertEquals(200, response.getCode()) ;
		Assertions.assertTrue(response.getBodyText().contains("HttpClient5Async/SRU")) ;
	}
	
	@Test
	public void testAsyncHttpClientTimeout() throws Exception {
		final SimpleHttpRequest httpGet = SimpleHttpRequest.create("GET", Constants.HTTPBIN_TEST_HOST + "/delay/4");
		Future<SimpleHttpResponse> future = async.execute(httpGet, new FutureCallback<SimpleHttpResponse>() {

			@Override
			public void completed(SimpleHttpResponse result) {
				Assertions.fail("Should have timeout") ;
			}

			@Override
			public void failed(Exception ex) {
				Assertions.assertTrue(ex instanceof HttpStreamResetException);
				Assertions.assertTrue(ex.getMessage().toLowerCase().contains("timeout"));
			}

			@Override
			public void cancelled() {
				Assertions.fail("Should not have been cancel") ;
			}
		});
		Thread.sleep(4000) ;
		boolean cancelled = future.isCancelled() ;
		boolean done = future.isDone() ;
		Assertions.assertFalse(cancelled) ;
		Assertions.assertTrue(done) ;
		
	}	
}
