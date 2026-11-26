package io.github.springboot.httpclient5.core.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ThreadFactory;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class ThreadFactoryUtilsTest {

	@Test
	void shouldCreateVirtualThreadsWhenAvailable() throws Exception {
		// Virtual threads are available from Java 19 onwards; the project still supports Java 17.
		Assumptions.assumeTrue(Runtime.version().feature() >= 19);

		ThreadFactory factory = ThreadFactoryUtils.getVirtualThreadFactoryViaReflection();

		Thread thread = factory.newThread(() -> {
		});

		assertNotNull(thread);
		// Thread.isVirtual() is not part of the Java 17 API used to compile the tests.
		assertTrue((Boolean) Thread.class.getMethod("isVirtual").invoke(thread));
	}
}
