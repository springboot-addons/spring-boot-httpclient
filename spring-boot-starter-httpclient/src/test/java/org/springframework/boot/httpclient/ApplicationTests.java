package org.springframework.boot.httpclient;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringRunner;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.springboot.httpclient.stats.actuator.HttpClientEndpoint;

/**
 * http client auto configuration tests
 *
 * @author linux_china
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = { "junit.testcase=true" }, webEnvironment = WebEnvironment.NONE)
@ComponentScan("io.github.springboot.httpclient")
public class ApplicationTests {

    @Autowired
    ApplicationContext context;

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    public void testHttpsTrustSpecificDomain() throws Exception {
        final HttpClient httpClient = context.getBean(HttpClient.class);
        final HttpGet httpGet = new HttpGet(
                "https://fr.news.yahoo.com/renault-r%C3%A9union-extraordinaire-ca-jeudi-soir-150308444.html");
        final HttpResponse response = httpClient.execute(httpGet);
        EntityUtils.toString(response.getEntity());
        dumpStats();
    }

    @Test
    public void testHttpsClient() throws Exception {
        final HttpClient httpClient = context.getBean(HttpClient.class);
        final HttpGet httpGet = new HttpGet("https://httpbin.org/headers");
        final HttpResponse response = httpClient.execute(httpGet);
        EntityUtils.toString(response.getEntity());
        dumpStats();
    }

    @Test
    public void testRateLimiter() throws Exception {
        final HttpClient httpClient = context.getBean(HttpClient.class);
        final HttpGet httpGet = new HttpGet("https://httpbin.org/headers");
        for (int i = 0; i < 12; i++) {
            final HttpResponse response = httpClient.execute(httpGet);
            if (response != null && response.getEntity() != null) {
                EntityUtils.toString(response.getEntity());
                dumpStats();
            }
        }
    }

    @Test
    public void testHttpClientSoTimeout() throws Exception {
        final HttpClient httpClient = context.getBean(HttpClient.class);
        final HttpGet httpGet = new HttpGet("https://httpbin.org/delay/4");
        try {
            final HttpResponse response = httpClient.execute(httpGet);
            Assert.fail("Timeout should have occured");
        } catch (final Exception e) {
            circuitBreakerRegistry.circuitBreaker("httpbin").reset();
        }
        dumpStats();
    }

    @Test
    public void testHttpClientPostSoTimeout() throws Exception {
        final HttpClient httpClient = context.getBean(HttpClient.class);
        final HttpPost httpPost = new HttpPost("https://httpbin.org/delay/4");
        try {
            final HttpResponse response = httpClient.execute(httpPost);
        } catch (final Exception e) {
            Assert.fail("Timeout not should have occured");
        }
        dumpStats();
    }

    @Test
    public void testExecutor() throws Exception {
        final Executor executor = context.getBean(Executor.class);
        final String content = executor.execute(Request.Get("https://httpbin.org/headers")).returnContent().asString();
        Assert.assertTrue(content.contains("httpclient"));
        dumpStats();
    }

    @Test
    public void testCircuitBreakerException() throws Exception {
        final HttpClient httpClient = context.getBean(HttpClient.class);
        final HttpGet httpGet = new HttpGet("https://www.unknown.com");
        boolean hasRecovered = false;
        boolean hasBeenBreaked = false;
        for (int i = 0; i < 4; i++) {
            try {
                final HttpResponse response = httpClient.execute(httpGet);
                System.out.println(response.getStatusLine());
                Assert.assertTrue(response.getStatusLine().getReasonPhrase().contains("Broken circuit"));
                Assert.assertTrue(response.getStatusLine().getStatusCode() == 503);
                hasBeenBreaked = true;
                Thread.sleep(3100);
            } catch (final Exception e) {
                hasRecovered = hasBeenBreaked;
                System.out.println("Error : " + e.getMessage());
            }
        }
        Assert.assertTrue(hasBeenBreaked);
        Assert.assertTrue(hasRecovered);

        dumpStats();
    }

    private void dumpStats() {
        final HttpClientEndpoint endpoint = context.getBean(HttpClientEndpoint.class);
        System.out.println(ToStringBuilder.reflectionToString(endpoint, ToStringStyle.JSON_STYLE));
    }

    @Test
    public void testCircuitBreakerHttp503() throws Exception {
        final HttpClient httpClient = context.getBean(HttpClient.class);
        final HttpGet httpGet = new HttpGet("https://httpbin.org/status/503");
        for (int i = 0; i < 5; i++) {
            final HttpResponse response = httpClient.execute(httpGet);
            Assert.assertTrue(response.getStatusLine().getStatusCode() == 503);
        }
        final HttpResponse response = httpClient.execute(httpGet);
        System.out.println(response.getStatusLine());
        Assert.assertTrue(response.getStatusLine().getReasonPhrase().contains("Broken circuit"));
        Assert.assertTrue(response.getStatusLine().getStatusCode() == 503);
    }

    @Test
    public void testSslInsee() throws Exception {
        final HttpClient httpClient = context.getBean(HttpClient.class);
        final HttpPost req = new HttpPost("https://api.insee.fr/token");
        final HttpResponse response = httpClient.execute(req);
        System.out.println(response.getStatusLine());
        Assert.assertTrue(response.getStatusLine().getStatusCode() == 400);
    }

    @Test
    public void testLegacyRateLimiterInsee() throws Exception {
        final Executor executor = context.getBean(Executor.class);
        for (int i = 0; i < 50; i++) {
            final HttpResponse content = executor.execute(Request.Post("https://api.insee.fr/token?i=" + i))
                    .returnResponse();
            System.out.println(IOUtils.toString(content.getEntity().getContent(), "UTF-8"));
            Assert.assertTrue(content.getStatusLine().getStatusCode() == 400);

        }
    }

}
