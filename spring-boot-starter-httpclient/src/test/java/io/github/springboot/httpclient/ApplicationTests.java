package io.github.springboot.httpclient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import io.github.springboot.httpclient.core.config.HttpClientConfigurationHelper;
import io.github.springboot.httpclient.core.config.model.ProxyConfiguration;
import lombok.extern.slf4j.Slf4j;

/**
 * http client auto configuration tests
 *
 * @author linux_china
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
@ComponentScan("io.github.springboot.httpclient.core")
@Slf4j
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
	public void testBasicAuth() throws Exception {
		final HttpClient httpClient = context.getBean(HttpClient.class);
		final HttpGet httpGet = new HttpGet(
				"https://httpbin.org/basic-auth/testusername/testpassword");
		final HttpResponse response = httpClient.execute(httpGet);
		assertTrue(response.getStatusLine().getStatusCode() == 200);
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
			fail("Timeout should have occured");
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
			fail("Timeout not should have occured");
		}
	}

	@Test
	public void testExecutor() throws Exception {
		final Executor executor = context.getBean(Executor.class);
		final String content = executor.execute(Request.Get("https://httpbin.org/headers")).returnContent().asString();
		assertTrue(content.contains("httpclient"));
	}

	@Test
	public void testCustomTlsDomainValidation() throws Exception {
		final HttpClient httpClient = context.getBean(HttpClient.class);
		final HttpPost req = new HttpPost("https://api.insee.fr/token");
		final HttpResponse response = httpClient.execute(req);
		log.info("Response statusLine is : '{}'", response.getStatusLine());
		assertTrue(response.getStatusLine().getStatusCode() == 400);
	}
	
	@Test
	public void testProxy() throws Exception {
		String uri = "https://www.impots.gouv.fr/portail/";
		final HttpClientConfigurationHelper configHelper = context.getBean(HttpClientConfigurationHelper.class);
		assertTrue(configHelper.useProxyForHost(uri));
		ProxyConfiguration proxyConfiguration = configHelper.getProxyConfiguration(uri);
		assertNotNull(proxyConfiguration);
		assertEquals(proxyConfiguration.getPort(), 8085);
		assertEquals(proxyConfiguration.getHost(), "localhost");
	}

}
