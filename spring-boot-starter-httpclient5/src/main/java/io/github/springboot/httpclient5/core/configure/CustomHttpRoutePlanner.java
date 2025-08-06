package io.github.springboot.httpclient5.core.configure;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver;
import org.apache.hc.client5.http.impl.routing.DefaultRoutePlanner;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public class CustomHttpRoutePlanner implements HttpRoutePlanner {

    private final HttpClient5Config config;

    private final HttpRoutePlanner delegate;

    public CustomHttpRoutePlanner(HttpClient5Config config) {
        this.config = config;
        this.delegate = new DefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE);
    }

    @Override
    public HttpRoute determineRoute(HttpHost target, HttpContext context) throws HttpException {
        return determineRoute(target, null, context);
    }

    @Override
    public HttpRoute determineRoute(HttpHost host, HttpRequest request, HttpContext context) throws HttpException {
        final HttpClientContext clientContext = HttpClientContext.cast(context);

        if (request != null) {
            try {
                URI uri = request.getUri();
                final String method = request.getMethod();

                RequestConfigProperties requestConfigProperties = config.getRequestConfigProperties(method, uri.toString());
                RequestConfig requestConfig = requestConfigProperties.build();
                clientContext.setRequestConfig(requestConfig);
            } catch (URISyntaxException e) {
                log.warn("Unable to configure httpclient request, no uri available : using defaut configuration", e);
            }
        }
        return delegate.determineRoute(host, request, clientContext);
    }
}
