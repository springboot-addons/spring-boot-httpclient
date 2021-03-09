package io.github.springboot.httpclient.internal;

import java.io.IOException;

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

public interface ChainableHttpRequestExecutor {

	public HttpResponse doExecute(final HttpRequest request, final HttpClientConnection conn, final HttpContext context,
			HttpRequestExecutorChain chain) throws IOException, HttpException;
}
