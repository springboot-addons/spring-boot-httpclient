package io.github.springboot.httpclient5.core.config.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.unit.DataSize;

@SpringBootTest(webEnvironment = WebEnvironment.NONE, properties = "test.timeout=10s")
class ConversionServiceConfigurationTest {

	@Value("${test.timeout}")
	private Duration timeout;

	@Autowired
	private ConversionService conversionService;

	@Test
	void shouldKeepSpringBootConverters() {
		assertEquals(Duration.ofSeconds(10), timeout);
		assertEquals(DataSize.ofMegabytes(10), conversionService.convert("10MB", DataSize.class));
		assertInstanceOf(ApplicationConversionService.class, conversionService);
	}

	@Test
	void shouldKeepStarterConverters() {
		assertTrue(conversionService.canConvert(String.class, HttpHost.class));
		assertEquals(Timeout.ofSeconds(2), conversionService.convert("PT2S", Timeout.class));
	}
}
