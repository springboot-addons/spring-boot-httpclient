package io.github.springboot.httpclient.core.interceptors;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;

public interface HttpClientInterceptor extends HttpRequestInterceptor, HttpResponseInterceptor {

}
