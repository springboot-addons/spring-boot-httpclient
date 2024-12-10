package io.github.springboot.httpclient5;

import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * http client auto configuration tests
 *
 * @author linux_china
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE, 
	properties = "spring.httpclient5.request-config[default].connection-request-timeout=PT2S")
@ActiveProfiles("test")
@Slf4j
public class ConfigTestsProperties {

	@Autowired
	HttpClient5Config config;

	
	@Value("${spring.httpclient5.request-config.default.connection-request-timeout}")
	Timeout defaultConnectionRequestTimeout  ;
	
	
	@Test
	public void testRequestConfigBinding() throws Exception {
		RequestConfig requestConfig = config.getRequestConfig("GET", Constants.HTTPBIN_TEST_HOST + "/test");

		Timeout connectionRequestTimeout = requestConfig.getConnectionRequestTimeout() ;
		Assertions.assertEquals(defaultConnectionRequestTimeout, connectionRequestTimeout) ;
		Assertions.assertEquals(2000, connectionRequestTimeout.toMilliseconds()) ;
	}
}
