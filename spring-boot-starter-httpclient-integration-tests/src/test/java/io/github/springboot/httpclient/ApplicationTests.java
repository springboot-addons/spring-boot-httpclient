package io.github.springboot.httpclient;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * http client auto configuration tests
 *
 * @author linux_china
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
@ComponentScan("io.github.springboot.httpclient.core")
public class ApplicationTests {

	@Autowired
	ApplicationContext context;

	@Test
	public void testHttpsTrustSpecificDomain() throws Exception {
		final HttpClient httpClient = context.getBean(HttpClient.class);
		final HttpGet httpGet = new HttpGet(
				"https://fr.news.yahoo.com/renault-r%C3%A9union-extraordinaire-ca-jeudi-soir-150308444.html");
		final HttpResponse response = httpClient.execute(httpGet);
		EntityUtils.toString(response.getEntity());
	}

	@Test
	public void testHttpsClient() throws Exception {
		final HttpClient httpClient = context.getBean(HttpClient.class);
		final HttpGet httpGet = new HttpGet("https://httpbin.org/headers");
		final HttpResponse response = httpClient.execute(httpGet);
		EntityUtils.toString(response.getEntity());
	}

	@Test
	public void testHttpClientSoTimeout() throws Exception {
		final HttpClient httpClient = context.getBean(HttpClient.class);
		final HttpGet httpGet = new HttpGet("https://httpbin.org/delay/4");
		try {
			httpClient.execute(httpGet);
			Assert.fail("Timeout should have occured");
		} catch (final Exception e) {
		}
	}

	@Test
	public void testHttpClientPostSoTimeout() throws Exception {
		final HttpClient httpClient = context.getBean(HttpClient.class);
		final HttpPost httpPost = new HttpPost("https://httpbin.org/delay/4");
		try {
			httpClient.execute(httpPost);
		} catch (final Exception e) {
			Assert.fail("Timeout not should have occured");
		}
	}

	@Test
	public void testExecutor() throws Exception {
		final Executor executor = context.getBean(Executor.class);
		final String content = executor.execute(Request.Get("https://httpbin.org/headers")).returnContent().asString();
		Assert.assertTrue(content.contains("httpclient"));
	}

	@Test
	public void testCustomTlsDomainValidation() throws Exception {
		final HttpClient httpClient = context.getBean(HttpClient.class);
		final HttpPost req = new HttpPost("https://api.insee.fr/token");
		final HttpResponse response = httpClient.execute(req);
		System.out.println(response.getStatusLine());
		Assert.assertTrue(response.getStatusLine().getStatusCode() == 400);
	}

}
