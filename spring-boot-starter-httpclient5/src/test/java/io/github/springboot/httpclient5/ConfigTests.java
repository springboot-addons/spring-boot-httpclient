package io.github.springboot.httpclient5;

import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
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
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
@Slf4j
public class ConfigTests {

	@Autowired
	HttpClient5Config config;

	@Autowired
	PoolingHttpClientConnectionManager cm;
	
	@Autowired
	PoolingAsyncClientConnectionManager asyncPoolCm;

	
	@Value("${spring.httpclient5.request-config.default.connection-request-timeout}")
	Timeout defaultConnectionRequestTimeout  ;
	
	@Test
	public void testRequestConfigKeyExpension() throws Exception {
		Assertions.assertTrue(config.getRequestConfig().containsKey("GET " + Constants.HTTPBIN_TEST_HOST + "/.*")) ;
		Assertions.assertTrue(config.getPool().getHostConfig().containsKey(Constants.HTTPBIN_TEST_HOST)) ;
	}
	
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
		Assertions.assertEquals(30, cm.getDefaultMaxPerRoute());
		Assertions.assertEquals(500, config.getPool().getDefaultConnectionConfig().getConnectTimeout().convert(TimeUnit.MILLISECONDS));
		
		String httpbinHostname = Constants.HTTPBIN_TEST_HOST_NAME ;
		int httpbinPost = Constants.HTTPBIN_TEST_PORT;
		String httpbinScheme = Constants.HTTPBIN_TEST_SCHEME;
		Assertions.assertEquals(10, cm.getMaxPerRoute(new HttpRoute(new HttpHost(httpbinScheme, httpbinHostname, httpbinPost), null, "https".equals(httpbinScheme)))) ;
		Assertions.assertEquals(30, cm.getMaxPerRoute(new HttpRoute(new HttpHost(httpbinScheme, httpbinHostname, httpbinPost), null, new HttpHost("https", "localhost", 3128), "https".equals(httpbinScheme)))) ;
		Assertions.assertEquals(20, cm.getMaxPerRoute(new HttpRoute(new HttpHost("https", "testhost", 4443), null, new HttpHost("https", "localhost", 3128), true))) ;
	}

	@Test
	public void testPoolingAsyncConnectionManagerConfig() throws Exception {
		Assertions.assertEquals(0, asyncPoolCm.getMaxTotal()) ;
		Assertions.assertEquals(20, asyncPoolCm.getDefaultMaxPerRoute());
		Assertions.assertEquals(100, config.getAsyncPool().getDefaultConnectionConfig().getConnectTimeout().convert(TimeUnit.MILLISECONDS));
		
		String httpbinHostname = Constants.HTTPBIN_TEST_HOST_NAME ;
		int httpbinPost = Constants.HTTPBIN_TEST_PORT;
		String httpbinScheme = Constants.HTTPBIN_TEST_SCHEME;
		Assertions.assertEquals(15, asyncPoolCm.getMaxPerRoute(new HttpRoute(new HttpHost(httpbinScheme, httpbinHostname, httpbinPost), null, "https".equals(httpbinScheme)))) ;
		Assertions.assertEquals(20, asyncPoolCm.getMaxPerRoute(new HttpRoute(new HttpHost(httpbinScheme, httpbinHostname, httpbinPost), null, new HttpHost("https", "localhost", 3128), "https".equals(httpbinScheme)))) ;
		Assertions.assertEquals(40, asyncPoolCm.getMaxPerRoute(new HttpRoute(new HttpHost("https", "testhost", 4443), null, new HttpHost("https", "localhost", 3128), true))) ;
	}

	@Test
	public void testInterceptorConfig() throws Exception {
		RequestConfigProperties someHostConfig = config.getRequestConfigProperties("GET", "https://somehost/test");
		RequestConfigProperties httpBinConfig = config.getRequestConfigProperties("GET", Constants.HTTPBIN_TEST_HOST + "/test");

		Assertions.assertFalse(someHostConfig.getInterceptors().get("myinter")) ; ;
		Assertions.assertTrue(someHostConfig.getCustomRequestContext().containsKey("propA")) ; ;

		Assertions.assertTrue(httpBinConfig.getInterceptors().get("myinter")) ; ;
		Assertions.assertTrue(httpBinConfig.getCustomRequestContext().containsKey("propB")) ; ;
		Assertions.assertEquals("surcharge", httpBinConfig.getCustomRequestContext().get("propA")) ; ;
	}


	@Test
	public void testRequestConfigBinding() throws Exception {
		RequestConfig requestConfig = config.getRequestConfig("GET", Constants.HTTPBIN_TEST_HOST + "/test");

		Timeout connectionRequestTimeout = requestConfig.getConnectionRequestTimeout() ;
		Assertions.assertEquals(defaultConnectionRequestTimeout, connectionRequestTimeout) ;
		Assertions.assertEquals(1000, connectionRequestTimeout.toMilliseconds()) ;

		Timeout reqponseTimeout = requestConfig.getResponseTimeout() ;
		Assertions.assertEquals(Timeout.ofSeconds(3), reqponseTimeout) ;
	}
}
