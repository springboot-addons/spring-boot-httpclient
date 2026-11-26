package io.github.springboot.httpclient.web.async.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ContextCopyingTaskDecoratorTest {

	private final AsyncExecutorConfig.ContextCopyingTaskDecorator decorator = new AsyncExecutorConfig.ContextCopyingTaskDecorator();

	@AfterEach
	void tearDown() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void shouldCopyRequestContextToTheAsyncThread() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-TEST-KEY", "srules");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		Runnable decorated = decorator.decorate(() -> {
			assertNotNull(RequestContextHolder.getRequestAttributes());
			ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			assertEquals("srules", attributes.getRequest().getHeader("X-TEST-KEY"));
		});

		// Remove the context from the current thread before running the decorated task
		RequestContextHolder.resetRequestAttributes();

		Thread thread = new Thread(decorated);
		thread.start();
		thread.join();

		assertNull(RequestContextHolder.getRequestAttributes());
	}

	@Test
	void shouldExecuteWhenNoRequestContextIsActive() {
		AtomicReference<Boolean> executed = new AtomicReference<>(false);

		Runnable decorated = decorator.decorate(() -> executed.set(true));

		decorated.run();
		assertTrue(executed.get());
	}

	@Test
	void shouldRestorePreviousContextAfterExecution() {
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

		Runnable decorated = decorator.decorate(() -> {
		});
		decorated.run();

		assertNotNull(RequestContextHolder.getRequestAttributes());
	}
}
