package io.github.springboot.httpclient5;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import lombok.extern.slf4j.Slf4j;

/**
 * http client auto configuration tests
 *
 * @author linux_china
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
@Slf4j
public class ConfigTests {

	@Autowired
	HttpClient5Config config;

	@Autowired
	PoolingHttpClientConnectionManager cm;
	
	@Test
	public void testPropertiesBinding() throws Exception {
		log.info("Config is {}", config);
		
		Assertions.assertEquals(4096, config.getHttp1().getBufferSize());
		Assertions.assertEquals(10, config.getHttp1().getMaxEmptyLineCount());
		Assertions.assertEquals(Timeout.ofSeconds(32), config.getHttp1().getWaitForContinueTimeout());
	}
	
	@Test
	public void testConnectionManagerConfig() throws Exception {
		Assertions.assertEquals(128, cm.getMaxTotal()) ;
		Assertions.assertEquals(5, cm.getDefaultMaxPerRoute());
		Assertions.assertEquals(10, cm.getMaxPerRoute(new HttpRoute(new HttpHost("httpbin.agglo-larochelle.fr", 443)))) ;
		Assertions.assertEquals(5, cm.getMaxPerRoute(new HttpRoute(new HttpHost("httpbin.agglo-larochelle.fr", 443), new HttpHost("https", "localhost", 3128)))) ;
		Assertions.assertEquals(20, cm.getMaxPerRoute(new HttpRoute(new HttpHost("testhost", 4443), new HttpHost("https", "localhost", 3128)))) ;
	}
	
}
