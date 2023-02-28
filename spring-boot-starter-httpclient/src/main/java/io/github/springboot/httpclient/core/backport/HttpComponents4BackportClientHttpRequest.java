/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.springboot.httpclient.core.backport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StringUtils;

/**
 * {@link ClientHttpRequest} implementation based on
 * Apache HttpComponents HttpClient.
 *
 * <p>Created via the {@link HttpComponentsClientHttpRequestFactory}.
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.1
 * @see HttpComponentsClientHttpRequestFactory#createRequest(URI, HttpMethod)
 */
class HttpComponents4BackportClientHttpRequest extends AbstractClientHttpRequest {

	private final HttpClient httpClient;

	private final HttpUriRequest httpRequest;

	private final HttpContext httpContext;


	HttpComponents4BackportClientHttpRequest(HttpClient client, HttpUriRequest request, HttpContext context) {
		this.httpClient = client;
		this.httpRequest = request;
		this.httpContext = context;
	}


	@Override
	public String getMethodValue() {
		return this.httpRequest.getMethod();
	}

	@Override
	public URI getURI() {
		return this.httpRequest.getURI();
	}

	HttpContext getHttpContext() {
		return this.httpContext;
	}


	protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
		addHeaders(this.httpRequest, headers);

		if (this.httpRequest instanceof HttpEntityEnclosingRequest) {
			HttpEntityEnclosingRequest entityEnclosingRequest = (HttpEntityEnclosingRequest) this.httpRequest;
			HttpEntity requestEntity = new ByteArrayEntity(bufferedOutput);
			entityEnclosingRequest.setEntity(requestEntity);
		}
		HttpResponse httpResponse = this.httpClient.execute(this.httpRequest, this.httpContext);
		return new HttpComponents4BackportClientHttpResponse(httpResponse);
	}


	/**
	 * Add the given headers to the given HTTP request.
	 * @param httpRequest the request to add the headers to
	 * @param headers the headers to add
	 */
	static void addHeaders(HttpUriRequest httpRequest, HttpHeaders headers) {
		headers.forEach((headerName, headerValues) -> {
			if (HttpHeaders.COOKIE.equalsIgnoreCase(headerName)) {  // RFC 6265
				String headerValue = StringUtils.collectionToDelimitedString(headerValues, "; ");
				httpRequest.addHeader(headerName, headerValue);
			}
			else if (!HTTP.CONTENT_LEN.equalsIgnoreCase(headerName) &&
					!HTTP.TRANSFER_ENCODING.equalsIgnoreCase(headerName)) {
				for (String headerValue : headerValues) {
					httpRequest.addHeader(headerName, headerValue);
				}
			}
		});
	}


	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(getMethodValue());
	}

	private ByteArrayOutputStream bufferedOutput = new ByteArrayOutputStream(1024);


	@Override
	protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
		return this.bufferedOutput;
	}

	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
		byte[] bytes = this.bufferedOutput.toByteArray();
		if (headers.getContentLength() < 0) {
			headers.setContentLength(bytes.length);
		}
		ClientHttpResponse result = executeInternal(headers, bytes);
		this.bufferedOutput = new ByteArrayOutputStream(0);
		return result;
	}
}
