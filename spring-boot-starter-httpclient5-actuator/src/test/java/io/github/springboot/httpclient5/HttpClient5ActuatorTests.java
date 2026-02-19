package io.github.springboot.httpclient5;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;


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
@Slf4j
public class HttpClient5ActuatorTests {
	
	@Autowired
	ApplicationContext context;
	
	@Autowired
	MeterRegistry stats ;
	
	@Test
	@Order(1)
	public void testHttpsClientWithStats() throws Exception {
		final CloseableHttpClient httpClient = context.getBean(CloseableHttpClient.class);
		final HttpGet httpGet = new HttpGet(Constants.HTTPBIN_TEST_HOST+"/headers");
		final CloseableHttpResponse response = httpClient.execute(httpGet);
		EntityUtils.toString(response.getEntity());
		
		Assertions.assertNotNull(stats);
		double connectionsCount = stats.find("httpcomponents.httpclient.pool.total.connections")
			.tags(Arrays.asList(Tag.of("httpclient", "httpclient5.pool"), Tag.of("state", "available")))
			.meter().measure().iterator().next().getValue() ;
		Assertions.assertEquals(1.0d, connectionsCount);


		AtomicReference<Double> requestCount = new AtomicReference<>(0d) ; 
		stats.find("httpcomponents.httpclient.request")
			.tags(Arrays.asList(Tag.of("target.host", "nas.capsi-informatique.fr")))
			.timer()
			.measure().forEach(m -> { 
				log.info("meter: {} {}", m.getStatistic(), m.getValue()) ;
				if (m.getStatistic().name().equalsIgnoreCase("count")) {
					requestCount.set(m.getValue()) ;
				}
			});
		Assertions.assertEquals(1d, requestCount.get());
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

}
