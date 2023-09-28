package io.github.springboot.httpclient5.core.config.interceptors;

import org.apache.hc.client5.http.config.RequestConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
public class HttpRequestConfigurerInterceptorTest {

	@Autowired
	HttpClient5Config configurer;
	
	@Test
	void testGetRequestConfig() {
		RequestConfig requestConfig = configurer.getRequestConfig("GET", "https://testhost/a");
		Assertions.assertEquals(1000, requestConfig.getConnectTimeout().toMilliseconds()) ;
		Assertions.assertEquals(1000, requestConfig.getResponseTimeout().toMilliseconds()) ;
	
		// Only connect timeout modified
		RequestConfig requestConfig2 = configurer.getRequestConfig("POST", "https://testhost/a");
		Assertions.assertEquals(2000, requestConfig2.getConnectTimeout().toMilliseconds()) ;
		Assertions.assertEquals(1000, requestConfig2.getResponseTimeout().toMilliseconds()) ;

		// reponse timeout modified
		RequestConfig requestConfig3 = configurer.getRequestConfig("POST", "https://testhost/subpath/b");
		Assertions.assertEquals(2000, requestConfig3.getConnectTimeout().toMilliseconds()) ;
		Assertions.assertEquals(3000, requestConfig3.getResponseTimeout().toMilliseconds()) ;

	}

}
