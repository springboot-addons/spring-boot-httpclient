package io.github.springboot.httpclient5;

import org.apache.hc.client5.http.fluent.Executor;
import org.apache.hc.client5.http.fluent.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.HeadersPropagationProperties;

/**
 * http client auto configuration tests
 *
 * @author linux_china
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
@ComponentScan("io.github.springboot.httpclient5.core")
public class WebTests {

	@Autowired
	Executor executor;

	@Autowired
	HttpClient5Config config;

	@Test
	public void testConfig() throws Exception {
		HeadersPropagationProperties headersPropagationProperties = config.getRequestConfigProperties("GET", "https://httpbin.agglo-larochelle.fr/headers").getHeadersPropagation() ;
		Assertions.assertNotNull(headersPropagationProperties) ;
		Assertions.assertTrue(headersPropagationProperties.getEnabled()) ;		
		Assertions.assertEquals("X-TEST-.*", headersPropagationProperties.getUp()) ;
	}
	
	@Test
	public void testExecutor() throws Exception {
		final String content = executor.execute(Request.get("https://httpbin.agglo-larochelle.fr/headers")).returnContent().asString();
		Assertions.assertTrue(content.contains("SRU ADDED HEADER"));
	}

}
