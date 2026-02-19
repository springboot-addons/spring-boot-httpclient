package io.github.springboot.httpclient5;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import io.github.springboot.httpclient5.core.utils.LoggingFutureCallback;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * http client auto configuration tests
 *
 * @author sru
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE, 
		properties = { "spring.httpclient5.user-agent=HttpClient5Async/SRU",
			    "spring.httpclient5.request-config.default.retry-config.max-retries=0"})
@ActiveProfiles("test")
@Slf4j
@TestMethodOrder(OrderAnnotation.class)
@DirtiesContext
public class AsyncActuatorTests {

	@Autowired
	CloseableHttpAsyncClient async;
	
	@Autowired
	MeterRegistry stats ;

	@Test
	@Order(1)
	public void testAsyncHttpClient() throws Exception {
		final SimpleHttpRequest httpGet = SimpleHttpRequest.create("GET", Constants.HTTPBIN_TEST_HOST + "/headers");
		Future<SimpleHttpResponse> future = async.execute(httpGet, LoggingFutureCallback.INSTANCE);
		
		SimpleHttpResponse response = future.get() ;
		Assertions.assertEquals(200, response.getCode()) ;
		Assertions.assertTrue(response.getBodyText().contains("HttpClient5Async/SRU")) ;
		Assertions.assertNotNull(stats);
		double connectionsCount = stats.find("httpcomponents.httpclient.pool.total.connections")
			.tags(Arrays.asList(Tag.of("httpclient", "httpclient5.async-pool"), Tag.of("state", "available")))
			.meter().measure().iterator().next().getValue() ;
		Assertions.assertEquals(1.0d, connectionsCount);


		AtomicReference<Double> requestCount = new AtomicReference<>(0d) ; 
		stats.find("http.client.request.duration")
				.tags(Arrays.asList(Tag.of("server.address", "nas.capsi-informatique.fr")))
				.meter().measure().forEach(m -> { 
					log.info("meter: {} {}", m.getStatistic(), m.getValue()) ;
					if (m.getStatistic().name().equalsIgnoreCase("count")) {
						requestCount.set(m.getValue()) ;
					}
				});
			Assertions.assertEquals(1d, requestCount.get());
	}
	
	@Test
	@Order(2)
	public void testAsyncHttpClientTimeout() throws Exception {
		final SimpleHttpRequest httpGet = SimpleHttpRequest.create("GET", Constants.HTTPBIN_TEST_HOST + "/delay/4");
		Future<SimpleHttpResponse> future = async.execute(httpGet, new FutureCallback<SimpleHttpResponse>() {

			@Override
			public void completed(SimpleHttpResponse result) {
				Assertions.fail("Should have timeout") ;
			}

			@Override
			public void failed(Exception ex) {
				Assertions.assertTrue(ex instanceof SocketTimeoutException);
			}

			@Override
			public void cancelled() {
				Assertions.fail("Should not have been cancel") ;
			}
		});
		Thread.sleep(4000) ;
		boolean cancelled = future.isCancelled() ;
		boolean done = future.isDone() ;
		Assertions.assertFalse(cancelled) ;
		Assertions.assertTrue(done) ;
		Assertions.assertNotNull(stats);

		AtomicReference<Double> requestCount = new AtomicReference<>(0d) ; 
		stats.find("http.client.request.duration")
				.tags(Arrays.asList(Tag.of("server.address", "nas.capsi-informatique.fr")))
				.meter().measure().forEach(m -> { 
					log.info("meter: {} {}", m.getStatistic(), m.getValue()) ;
					if (m.getStatistic().name().equalsIgnoreCase("count")) {
						requestCount.set(m.getValue()) ;
					}
				});
		Assertions.assertEquals(1d, requestCount.get());
	}

}
