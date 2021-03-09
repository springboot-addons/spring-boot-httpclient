package io.github.springboot.httpclient.core.interceptors.impl;

import java.io.IOException;

import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.DeflateDecompressingEntity;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.protocol.HttpContext;

import io.github.springboot.httpclient.core.constants.HttpClientConstants;
import io.github.springboot.httpclient.core.interceptors.HttpClientInterceptor;

public class ContentEncodingInterceptor implements HttpClientInterceptor {

	/**
	 * @param config
	 */
	public ContentEncodingInterceptor() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(final HttpRequest httprequest, final HttpContext httpcontext)
			throws HttpException, IOException {
		if (!httprequest.containsHeader(HttpClientConstants.ACCEPT_ENCODING)) {
			httprequest.addHeader(HttpClientConstants.ACCEPT_ENCODING, HttpClientConstants.ACCEPT_ENCODING_GZIP);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(final HttpResponse response, final HttpContext httpcontext) throws HttpException, IOException {
		final HttpEntity entity = response.getEntity();
		if (entity != null) {
			final org.apache.http.Header ceheader = entity.getContentEncoding();
			if (ceheader != null) {
				final HeaderElement[] codecs = ceheader.getElements();
				for (int i = 0; i < codecs.length; i++) {
					if (codecs[i].getName().equalsIgnoreCase(HttpClientConstants.CONTENT_ENCODING_GZIP)) {
						response.setEntity(new GzipDecompressingEntity(response.getEntity()));
						return;
//          } else if (codecs[i].getName().equalsIgnoreCase(HttpClientConstants.CONTENT_ENCODING_BROTLI)) {
//            response.setEntity(new BrotliDecompressingEntity(response.getEntity()));
//            return;
					} else if (codecs[i].getName().equalsIgnoreCase(HttpClientConstants.CONTENT_ENCODING_DEFLATE)) {
						response.setEntity(new DeflateDecompressingEntity(response.getEntity()));
						return;
					}
				}
			}
		}
	}
}
