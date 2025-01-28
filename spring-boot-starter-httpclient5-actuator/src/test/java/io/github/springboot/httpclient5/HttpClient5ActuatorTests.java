package io.github.springboot.httpclient5;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.fluent.Executor;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import io.github.springboot.httpclient5.actuator.HttpClientEndpoint;

/**
 * http client auto configuration tests
 *
 * @author linux_china
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
@ComponentScan("io.github.springboot.httpclient5.core")
@TestMethodOrder(OrderAnnotation.class)
@DirtiesContext
public class HttpClient5ActuatorTests {
	
	@Autowired
	ApplicationContext context;

	@Test
	@Order(1)
	public void testHttpsClientWithStats() throws Exception {
		final CloseableHttpClient httpClient = context.getBean(CloseableHttpClient.class);
		final HttpGet httpGet = new HttpGet(Constants.HTTPBIN_TEST_HOST+"/headers");
		final CloseableHttpResponse response = httpClient.execute(httpGet);
		EntityUtils.toString(response.getEntity());
		HttpClientEndpoint stats = getStats();
		Assertions.assertNotNull(stats);
		Assertions.assertNotNull(stats.getMetrics());
		
		long requestCount = (long) stats.getMetrics().get("org.apache.hc.client5.http.classic.HttpClient.httpbin.org.get-requests.count") ;
		Assertions.assertEquals(1, requestCount);
	}

	@Test
	@Order(2)
	public void testHttpClientPostSoTimeout() throws Exception {
		final HttpClient httpClient = context.getBean(HttpClient.class);
		final HttpPost httpPost = new HttpPost(Constants.HTTPBIN_TEST_HOST+"/delay/4");
		try {
			httpClient.execute(httpPost);
		} catch (final Exception e) {
			Assertions.fail("Timeout not should have occured");
		}
	}

	@Test
	@Order(3)
	public void testExecutor() throws Exception {
		final Executor executor = context.getBean(Executor.class);
		final String content = executor.execute(Request.get(Constants.HTTPBIN_TEST_HOST+"/headers")).returnContent().asString();
		System.out.println(content);
		Assertions.assertTrue(content.contains("User-Agent"));
	}

	private HttpClientEndpoint getStats() {
		final HttpClientEndpoint endpoint = context.getBean(HttpClientEndpoint.class);
		return endpoint;
	}
}
