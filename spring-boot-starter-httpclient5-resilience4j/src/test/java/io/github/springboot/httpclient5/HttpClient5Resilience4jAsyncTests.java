package io.github.springboot.httpclient5;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.fluent.Executor;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.springboot.httpclient5.core.utils.LoggingFutureCallback;
import lombok.extern.slf4j.Slf4j;

/**
 * http client auto configuration tests
 *
 * @author linux_china
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE,
	properties = { "spring.httpclient5.user-agent=HttpClient5Async/SRU",
	"spring.httpclient5.request-config.default.retry-config.max-retries=0"})
@ActiveProfiles("test")
@ComponentScan("io.github.springboot.httpclient5.core")
@DirtiesContext
@Slf4j
public class HttpClient5Resilience4jAsyncTests {
	private static final String HTTPBIN_HOST = "http://nas.capsi-informatique.fr:9999";

	@Autowired
	ApplicationContext context;

	@Autowired
	CircuitBreakerRegistry circuitBreakerRegistry;

	@Test
	public void testRateLimiter() throws Exception {
		final CloseableHttpAsyncClient httpClient = context.getBean(CloseableHttpAsyncClient.class);
		
		final SimpleHttpRequest httpGet = SimpleHttpRequest.create("GET", HTTPBIN_HOST+ "/headers");
		long begin = System.currentTimeMillis() ;
		Stream.generate(() -> "noop").limit(12)
			.map(s -> httpClient.execute(httpGet, LoggingFutureCallback.INSTANCE))
			.toList().forEach(t -> {
				try {
					t.get();
				} catch (InterruptedException | ExecutionException e) {
					log.error("Failed ", e) ;
					//Assertions.fail(e) ; 
				}
			}) ;

		long end = System.currentTimeMillis() ;
		log.debug("Done 12 req in "+ (end - begin) + "ms");
		Assertions.assertTrue(end - begin > 10000, "Limit rate not ensured") ; // rate limit 10 req / 10 s
		circuitBreakerRegistry.find("httpbin-org").get().reset();
	}


	@Test
	public void testCircuitBreakerException() throws Exception {
		final CloseableHttpAsyncClient httpClient = context.getBean(CloseableHttpAsyncClient.class);
		final SimpleHttpRequest httpGet = SimpleHttpRequest.create("GET", "https://test.unknown-azerty.com");

		boolean hasBeenBreaked = false;
		for (int i = 0; i < 4; i++) {
			try {
				Future<SimpleHttpResponse> future = httpClient.execute(httpGet, LoggingFutureCallback.INSTANCE);
				SimpleHttpResponse response = future.get() ;
				if (response.getCode() > 500) {
					hasBeenBreaked = true;
				}
				else {
					Assertions.fail("Should not have been sucess") ;
				}
			} catch (final Exception e) {
				if (e.getMessage().toLowerCase().contains("broken circuit")) {
					hasBeenBreaked = true;
				}
				log.info("TU -> Error : " + e.getMessage());
			}
		}
		Assertions.assertTrue(hasBeenBreaked);
		circuitBreakerRegistry.find("default").get().reset();
	}

	@Test
	public void testCircuitBreakerHttp503() throws Exception {
		final CloseableHttpAsyncClient httpClient = context.getBean(CloseableHttpAsyncClient.class);
		final SimpleHttpRequest httpGet = SimpleHttpRequest.create("GET", HTTPBIN_HOST+ "/status/503");
		for (int i = 0; i < 3; i++) {
			Future<SimpleHttpResponse> future = httpClient.execute(httpGet, LoggingFutureCallback.INSTANCE);
			SimpleHttpResponse response = future.get() ;
			Assertions.assertTrue(response.getCode() == 503);
		}
		HttpResponse response;
		try {
			Future<SimpleHttpResponse> future = httpClient.execute(httpGet, LoggingFutureCallback.INSTANCE);
			response = future.get() ;
			Assertions.fail("Circuit breaker should have been used") ;
		} catch (Exception e) {
			Assertions.assertTrue(e.getMessage().toLowerCase().contains("broken circuit"));
		}
		circuitBreakerRegistry.find("httpbin-org").get().reset();
	}
}
