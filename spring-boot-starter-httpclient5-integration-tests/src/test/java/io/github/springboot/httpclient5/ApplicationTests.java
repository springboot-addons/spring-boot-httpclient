package io.github.springboot.httpclient5;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

/**
 * http client auto configuration tests
 *
 * @author linux_china
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
@ComponentScan("io.github.springboot.httpclient5.core")
public class ApplicationTests {

	@Autowired
	ApplicationContext context;
	
	@Autowired
	CloseableHttpClient httpClient;

	@Test
	public void testHttpsTrustSpecificDomain() throws Exception {
		final HttpGet httpGet = new HttpGet(
				"https://fr.news.yahoo.com/renault-r%C3%A9union-extraordinaire-ca-jeudi-soir-150308444.html");
		final CloseableHttpResponse response = httpClient.execute(httpGet);
		EntityUtils.toString(response.getEntity());
	}

	@Test
	public void testHttpsClient() throws Exception {
		final HttpGet httpGet = new HttpGet("https://httpbin.agglo-larochelle.fr/headers");
		final CloseableHttpResponse response = httpClient.execute(httpGet);
		EntityUtils.toString(response.getEntity());
	}

	@Test
	public void testHttpClientSoTimeout() throws Exception {
		final HttpGet httpGet = new HttpGet("https://httpbin.agglo-larochelle.fr/delay/4");
		try {
			httpClient.execute(httpGet);
			Assertions.fail("Timeout should have occured");
		} catch (final Exception e) {
		}
	}

	@Test
	public void testHttpClientPostSoTimeout() throws Exception {
		final HttpClient httpClient = context.getBean(HttpClient.class);
		final HttpPost httpPost = new HttpPost("https://httpbin.agglo-larochelle.fr/delay/4");
		try {
			httpClient.execute(httpPost);
		} catch (final Exception e) {
			Assertions.fail("Timeout not should have occured");
		}
	}

	@Test
	public void testExecutor() throws Exception {
		final Executor executor = context.getBean(Executor.class);
		final String content = executor.execute(Request.get("https://httpbin.agglo-larochelle.fr/headers")).returnContent().asString();
		Assertions.assertTrue(content.contains("User-Agent"));
	}

	@Test
	public void testCustomTlsDomainValidation() throws Exception {
		final HttpPost req = new HttpPost("https://api.insee.fr/token");
		final HttpResponse response = httpClient.execute(req);
		System.out.println(response.getReasonPhrase());
		Assertions.assertTrue(response.getCode() == 400);
	}

}
