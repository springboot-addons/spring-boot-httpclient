package io.github.springboot.httpclient5;

import java.io.IOException;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.fluent.Executor;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * http client auto configuration tests
 *
 * @author linux_china
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
@DirtiesContext
public class HttpClient5Resilience4jTests {

	@Autowired
	ApplicationContext context;

	@Autowired
	CircuitBreakerRegistry circuitBreakerRegistry;

	@Test
    @Order(4)
	public void testRateLimiter() throws Exception {
		final CloseableHttpClient httpClient = context.getBean(CloseableHttpClient.class);
		final HttpGet httpGet = new HttpGet(Constants.HTTPBIN_TEST_HOST + "/headers");
		long begin = System.currentTimeMillis() ;
		for (int i = 0; i < 12; i++) {
			final CloseableHttpResponse response = httpClient.execute(httpGet);
			if (response != null && response.getEntity() != null) {
				EntityUtils.toString(response.getEntity());
			}
		}
		long end = System.currentTimeMillis() ;
		System.out.println("Done 12 req in "+ (end - begin) + "ms");
		Assertions.assertTrue(end - begin > 10000) ; // rate limit 10 req / 10 s
		circuitBreakerRegistry.find("httpbin-org").get().reset();
	}

	@Test
    @Order(1)
	public void testHttpClientPostSoTimeout() throws Exception {
		final HttpClient httpClient = context.getBean(HttpClient.class);
		final HttpPost httpPost = new HttpPost(Constants.HTTPBIN_TEST_HOST + "/delay/4");
		try {
			httpClient.execute(httpPost);
		} catch (final Exception e) {
			Assertions.fail("Timeout not should have occured");
		}finally {
			circuitBreakerRegistry.find("httpbin-org").get().reset();
		}
	}

	@Test
    @Order(2)
	public void testExecutor() throws Exception {
		final Executor executor = context.getBean(Executor.class);
		final String content = executor.execute(Request.get(Constants.HTTPBIN_TEST_HOST + "/headers")).returnContent().asString();
		Assertions.assertTrue(content.contains("User-Agent"));
		circuitBreakerRegistry.find("httpbin-org").get().reset();
	}

	@Test
    @Order(5)
	public void testCircuitBreakerException() throws Exception {
		final CloseableHttpClient httpClient = context.getBean(CloseableHttpClient.class);
		final HttpGet httpGet = new HttpGet("https://test.unknown-azerty.com");
		boolean hasRecovered = false;
		boolean hasBeenBreaked = false;
		for (int i = 0; i < 4; i++) {
			try {
				final HttpResponse response = httpClient.execute(httpGet);
				System.out.println("TU -> " +response.getReasonPhrase());
				Assertions.assertTrue(response.getReasonPhrase().contains("Broken circuit"));
				Assertions.assertTrue(response.getCode() == 503);
				hasBeenBreaked = true;
				Thread.sleep(3100);
			} catch (final Exception e) {
				if (e instanceof IOException && e.getMessage().contains("Broken circuit")) {
					hasBeenBreaked = true;
				}
				hasRecovered = hasBeenBreaked;
				System.out.println("TU -> Error : " + e.getMessage());
			}
		}
		Assertions.assertTrue(hasBeenBreaked);
		Assertions.assertTrue(hasRecovered);
		circuitBreakerRegistry.find("default").get().reset();
	}

	@Test
    @Order(3)
	public void testCircuitBreakerHttp503() throws Exception {
		final HttpClient httpClient = context.getBean(HttpClient.class);
		final HttpGet httpGet = new HttpGet(Constants.HTTPBIN_TEST_HOST + "/status/503");
		for (int i = 0; i < 3; i++) {
			final HttpResponse response = httpClient.execute(httpGet);
			Assertions.assertTrue(response.getCode() == 503);
		}
		try {
			HttpResponse response = httpClient.execute(httpGet);
			Assertions.fail("Circuit breaker should have been used") ;
		} catch (IOException e) {
			Assertions.assertTrue(e.getMessage().contains("Broken circuit"));
		}
		circuitBreakerRegistry.find("httpbin-org").get().reset();
	}
}
