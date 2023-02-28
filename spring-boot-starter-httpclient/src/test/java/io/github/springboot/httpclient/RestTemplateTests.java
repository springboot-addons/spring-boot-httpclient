package io.github.springboot.httpclient;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * http client auto configuration tests
 *
 * @author sru
 */
@SpringBootTest(properties = "httpclient.core.rest-template-httpclient4.enabled=true",
		webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
@Slf4j
public class RestTemplateTests {

	@Autowired
	RestTemplate rest;

	@Test
	public void testRestTemplate() throws Exception {
		try {
			String res = rest.getForObject("https://httpbin.agglo-larochelle.fr/headers", String.class) ;
			Assertions.assertTrue(res.contains("User-Agent")) ;
		} catch (final Exception e) {
			fail("should not have occured", e);
		}
	}

	@Test
	public void testRestTemplateSoTimeout() throws Exception {
		try {
			rest.getForObject("https://httpbin.agglo-larochelle.fr/delay/4", String.class) ;
			fail("Timeout should have occured");
		} catch (final Exception e) {
		}
	}
}
