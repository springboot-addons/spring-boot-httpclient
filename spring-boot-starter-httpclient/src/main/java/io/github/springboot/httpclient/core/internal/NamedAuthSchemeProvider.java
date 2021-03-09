package io.github.springboot.httpclient.core.internal;

import org.apache.http.auth.AuthSchemeProvider;

import lombok.Data;

@Data
public class NamedAuthSchemeProvider {
	final private String name;
	final private AuthSchemeProvider provider;
}
