package io.github.springboot.httpclient5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import io.github.springboot.httpclient5.actuator.adaptive.MicrometerAdaptiveTimeoutPolicy;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@SpringBootTest(webEnvironment = WebEnvironment.NONE, properties = {
		"spring.httpclient5.adaptive-timeout.enabled=true",
		"spring.httpclient5.adaptive-timeout.warmup-requests=1",
		"spring.httpclient5.adaptive-timeout.min-samples=1",
		"spring.httpclient5.adaptive-timeout.max-samples=10",
		"spring.httpclient5.adaptive-timeout.window=PT1M",
		"spring.httpclient5.adaptive-timeout.refresh-interval=PT0.01S",
		"spring.httpclient5.adaptive-timeout.min-timeout=PT2S",
		"spring.httpclient5.adaptive-timeout.max-timeout=PT2S" })
@ActiveProfiles("test")
@DirtiesContext
class AdaptiveTimeoutActuatorTests extends AbstractHttpBinTest {

	@Autowired
	private CloseableHttpClient httpClient;

	@Autowired
	private MicrometerAdaptiveTimeoutPolicy policy;

	@Autowired
	private MeterRegistry meterRegistry;

	@Test
	void shouldApplyAdaptiveTimeoutAndExposeMetrics() throws Exception {
		executeGet("/headers");
		assertTrue(Double.isNaN(policy.getCurrentTimeoutSeconds()));

		executeGet("/headers");

		assertEquals(2.0d, policy.getCurrentTimeoutSeconds(), 0.001d);
		Gauge gauge = meterRegistry.find("httpclient5.adaptive.timeout").gauge();
		assertNotNull(gauge);
		assertEquals(2.0d, gauge.value(), 0.001d);
		assertTrue(policy.getCurrentSamples() > 0);
		assertTrue(policy.getTotalSamples() >= 2);
	}

	private void executeGet(String path) throws Exception {
		HttpGet request = new HttpGet(Constants.HTTPBIN_TEST_HOST + path);
		try (CloseableHttpResponse response = httpClient.execute(request)) {
			EntityUtils.consume(response.getEntity());
		}
	}
}
