package io.github.springboot.httpclient5;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for Spring tests relying on the local httpbin container.
 */
public abstract class AbstractHttpBinTest {

	@DynamicPropertySource
	static void httpBinProperties(DynamicPropertyRegistry registry) {
		registry.add("app.httpbin-host", HttpBinTestSupport::url);
	}
}
