package io.github.springboot.httpclient5.core.config;

import io.github.springboot.httpclient5.core.config.model.RequestConfigProperties;

public interface RequestConfigCustomizer {
	public static RequestConfigCustomizer NOOP = new RequestConfigCustomizer() {};
	
	default public RequestConfigProperties customizeRequestConfigProperties(String method, String uri, RequestConfigProperties defaults) { return defaults ;}
}
