package io.github.springboot.httpclient.core.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class ConfigKey {

	private String uri;
	private String method;
	private final String key;

}
