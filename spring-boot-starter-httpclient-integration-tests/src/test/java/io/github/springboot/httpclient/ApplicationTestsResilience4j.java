package io.github.springboot.httpclient;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * http client auto configuration tests
 *
 * @author linux_china
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
@ComponentScan("io.github.springboot.httpclient.core")
public class ApplicationTestsResilience4j {

	@Autowired
	ApplicationContext context;

	@Autowired
	CircuitBreakerRegistry circuitBreakerRegistry;

	@Test
	public void testRateLimiter() throws Exception {
		long begin = System.currentTimeMillis() ;
		final HttpClient httpClient = context.getBean(HttpClient.class);
		final HttpGet httpGet = new HttpGet("https://httpbin.org/headers");
		for (int i = 0; i < 15; i++) {
			final HttpResponse response = httpClient.execute(httpGet);
			if (response != null && response.getEntity() != null) {
				EntityUtils.toString(response.getEntity());
			}
		}
		long end = System.currentTimeMillis() ;
		Assertions.assertTrue(end-begin > 10000);
		
	}

	@Test
	public void testHttpClientPostSoTimeout() throws Exception {
		final HttpClient httpClient = context.getBean(HttpClient.class);
		final HttpPost httpPost = new HttpPost("https://httpbin.org/delay/4");
		try {
			httpClient.execute(httpPost);
		} catch (final Exception e) {
			Assertions.fail("Timeout not should have occured");
		}
	}

	@Test
	public void testExecutor() throws Exception {
		final Executor executor = context.getBean(Executor.class);
		final String content = executor.execute(Request.Get("https://httpbin.org/headers")).returnContent().asString();
		Assertions.assertTrue(content.contains("httpclient"));
	}

	@Test
	public void testCircuitBreakerException() throws Exception {
		final CloseableHttpClient httpClient = context.getBean(CloseableHttpClient.class);
		final HttpGet httpGet = new HttpGet("https://www.unknown.com");
		boolean hasRecovered = false;
		boolean hasBeenBreaked = false;
		for (int i = 0; i < 4; i++) {
			try {
				final HttpResponse response = httpClient.execute(httpGet);
				System.out.println(response.getStatusLine());
				Assertions.assertTrue(response.getStatusLine().getReasonPhrase().contains("Broken circuit"));
				Assertions.assertTrue(response.getStatusLine().getStatusCode() == 503);
				hasBeenBreaked = true;
				Thread.sleep(3100);
			} catch (final Exception e) {
				if (e instanceof IOException && e.getMessage().contains("Broken circuit")) {
					hasBeenBreaked = true;
				}
				hasRecovered = hasBeenBreaked;
				System.out.println("Error : " + e.getMessage());
			}
		}
		Assertions.assertTrue(hasBeenBreaked);
		Assertions.assertTrue(hasRecovered);
	}

	@Test
	public void testCircuitBreakerHttp503() throws Exception {
		final HttpClient httpClient = context.getBean(HttpClient.class);
		final HttpGet httpGet = new HttpGet("https://httpbin.org/status/503");
		for (int i = 0; i < 5; i++) {
			final HttpResponse response = httpClient.execute(httpGet);
			Assertions.assertTrue(response.getStatusLine().getStatusCode() == 503);
		}
		final HttpResponse response = httpClient.execute(httpGet);
		System.out.println(response.getStatusLine());
		Assertions.assertTrue(response.getStatusLine().getReasonPhrase().contains("Broken circuit"));
		Assertions.assertTrue(response.getStatusLine().getStatusCode() == 503);
		circuitBreakerRegistry.find("httpbin-org").get().reset();
	}
}
