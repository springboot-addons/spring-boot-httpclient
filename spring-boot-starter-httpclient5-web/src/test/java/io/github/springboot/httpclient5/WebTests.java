package io.github.springboot.httpclient5;

import java.util.concurrent.Future;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.fluent.Executor;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.HeadersPropagationProperties;
import io.github.springboot.httpclient5.core.utils.LoggingFutureCallback;

/**
 * http client auto configuration tests
 *
 * @author linux_china
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
@ComponentScan("io.github.springboot.httpclient5.core")
public class WebTests {
	private static final String HTTPBIN_HOST = "http://nas.capsi-informatique.fr:9999";

	
	@Autowired
	Executor executor;

	@Autowired
	CloseableHttpAsyncClient asyncHttpClient ;
	
	@Autowired
	HttpClient5Config config;

	@Test
	public void testConfig() throws Exception {
		HeadersPropagationProperties headersPropagationProperties = config.getRequestConfigProperties("GET", HTTPBIN_HOST+ "/headers").getHeadersPropagation() ;
		Assertions.assertNotNull(headersPropagationProperties) ;
		Assertions.assertTrue(headersPropagationProperties.getEnabled()) ;		
		Assertions.assertEquals("X-TEST-.*", headersPropagationProperties.getUp().get(0)) ;
	}
	
	@Test
	public void testExecutor() throws Exception {
		final String content = executor.execute(Request.get(HTTPBIN_HOST+ "/headers")).returnContent().asString();
		Assertions.assertTrue(content.contains("SRU ADDED HEADER"));
	}
	
	@Test
	public void testAsync() throws Exception {
		final SimpleHttpRequest httpGet = SimpleHttpRequest.create("GET", HTTPBIN_HOST+ "/headers");
		Future<SimpleHttpResponse> future = asyncHttpClient.execute(httpGet, LoggingFutureCallback.INSTANCE);
		SimpleHttpResponse response = future.get() ;
		Assertions.assertTrue(response.getBodyText().contains("SRU ADDED HEADER"));
	}


}
