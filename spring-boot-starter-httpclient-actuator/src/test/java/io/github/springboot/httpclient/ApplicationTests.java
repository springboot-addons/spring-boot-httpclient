package io.github.springboot.httpclient;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
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

import io.github.springboot.httpclient.actuator.HttpClientEndpoint;

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

    @Test
    public void testHttpsClientWithStats() throws Exception {
        final HttpClient httpClient = context.getBean(HttpClient.class);
        final HttpGet httpGet = new HttpGet("https://httpbin.org/headers");
        final HttpResponse response = httpClient.execute(httpGet);
        EntityUtils.toString(response.getEntity());
        HttpClientEndpoint stats = getStats();
        Assert.assertNotNull(stats);
        Assert.assertNotNull(stats.getMetrics());
    }

    @Test
    public void testHttpClientPostSoTimeout() throws Exception {
        final HttpClient httpClient = context.getBean(HttpClient.class);
        final HttpPost httpPost = new HttpPost("https://httpbin.org/delay/4");
        try {
            httpClient.execute(httpPost);
        } catch (final Exception e) {
            Assert.fail("Timeout not should have occured");
        }
    }

    @Test
    public void testExecutor() throws Exception {
        final Executor executor = context.getBean(Executor.class);
        final String content = executor.execute(Request.Get("https://httpbin.org/headers")).returnContent().asString();
        Assert.assertTrue(content.contains("httpclient"));
    }


    private HttpClientEndpoint getStats() {
        final HttpClientEndpoint endpoint = context.getBean(HttpClientEndpoint.class);
        System.out.println(ToStringBuilder.reflectionToString(endpoint, ToStringStyle.JSON_STYLE));
        return endpoint;
    }
}
