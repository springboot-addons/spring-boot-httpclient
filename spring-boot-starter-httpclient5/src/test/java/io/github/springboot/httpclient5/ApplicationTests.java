package io.github.springboot.httpclient5;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.fluent.Executor;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import lombok.extern.slf4j.Slf4j;

/**
 * http client auto configuration tests
 *
 * @author sru
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
@Slf4j
public class ApplicationTests {

	@Autowired
	ApplicationContext context;

	@Test
	public void testHttpsClient() throws Exception {
		final CloseableHttpClient httpClient = context.getBean(CloseableHttpClient.class);
		final HttpGet httpGet = new HttpGet("https://httpbin.agglo-larochelle.fr/headers");
		final CloseableHttpResponse response = httpClient.execute(httpGet);
		EntityUtils.toString(response.getEntity());
	}

	@Test
	public void testHttpClientSoTimeout() throws Exception {
		final CloseableHttpClient httpClient = context.getBean(CloseableHttpClient.class);
		final HttpGet httpGet = new HttpGet("https://httpbin.agglo-larochelle.fr/delay/4");
		try {
			httpClient.execute(httpGet);
			fail("Timeout should have occured");
		} catch (final Exception e) {
		}
	}

	@Test
	public void testHttpClientPostSoTimeout() throws Exception {
		final CloseableHttpClient httpClient = context.getBean(CloseableHttpClient.class);
		final HttpPost httpPost = new HttpPost("https://httpbin.agglo-larochelle.fr/delay/4");
		try {
			httpClient.execute(httpPost);
		} catch (final Exception e) {
			fail("Timeout not should have occured");
		}
	}
	
	@Test
	public void testHttpClientBasicAuth() throws Exception {
		final Executor executor = context.getBean(Executor.class);
		final String content = executor.execute(Request.get("https://httpbin.agglo-larochelle.fr/basic-auth/admin/pwd")).returnContent().asString();
		assertTrue(content.contains("authenticated"));
	}
	
	@Test
	public void testHttpClientPreemptiveBasicAuth() throws Exception {
		final Executor executor = context.getBean(Executor.class);
		final String content = executor.execute(Request.get("https://httpbin.agglo-larochelle.fr/hidden-basic-auth/admin/pwd")).returnContent().asString();
		assertTrue(content.contains("authenticated"));
	}
	
	@Test
	public void testExecutor() throws Exception {
		final Executor executor = context.getBean(Executor.class);
		final String content = executor.execute(Request.get("https://httpbin.agglo-larochelle.fr/headers")).returnContent().asString();
		assertTrue(content.contains("User-Agent"));
	}
	
	@Test
	public void testRetry() throws Exception {
		final Executor executor = context.getBean(Executor.class);
		final int code = executor.execute(Request.get("https://httpbin.agglo-larochelle.fr/status/503")).returnResponse().getCode();
		assertEquals(503, code);
	}
}
