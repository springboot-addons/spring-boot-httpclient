package io.github.springboot.httpclient5;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.fluent.Executor;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

/**
 * http client auto configuration tests
 *
 * @author linux_china
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("none")
public class ZeroConfTests {

	@Autowired
	CloseableHttpClient httpClient;
	
	@Autowired
	Executor executor ;

	@Test
	public void testHttpsClient() throws Exception {
		final HttpGet httpGet = new HttpGet(Constants.HTTPBIN_TEST_HOST + "/headers");
		final CloseableHttpResponse response = httpClient.execute(httpGet);
		EntityUtils.toString(response.getEntity());
	}

	@Test
	public void testExecutor() throws Exception {
		final String content = executor.execute(Request.get(Constants.HTTPBIN_TEST_HOST + "/headers")).returnContent().asString();
		Assertions.assertTrue(content.contains("User-Agent"));
	}
}
