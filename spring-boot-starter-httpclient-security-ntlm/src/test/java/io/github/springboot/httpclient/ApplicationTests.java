package io.github.springboot.httpclient;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.ComponentScan;

/**
 * http client auto configuration tests
 *
 * @author srouthiau
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ComponentScan("io.github.springboot.httpclient.core")
public class ApplicationTests {

	@Test
	public void testNtlm() throws Exception {
		// TBD
	}
}
