package io.github.springboot.httpclient;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

/**
 * http client auto configuration tests
 *
 * @author srouthiau
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
@ComponentScan("io.github.springboot.httpclient")
public class ApplicationTests {
	@Autowired
	ApplicationContext context;

	@Test
	public void testCas() throws Exception {
		// TBD
	}

	@Test
	public void testExecutor() throws Exception {
		final Executor executor = context.getBean(Executor.class);
		final String content = executor.execute(Request.Get("https://httpbin.org/headers")).returnContent().asString();
		Assert.assertTrue(content.contains("httpclient"));
	}
}
