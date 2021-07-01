package io.github.springboot.httpclient.web.headers;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.github.springboot.httpclient.core.interceptors.headers.HeadersPropagationConfig;
import io.github.springboot.httpclient.core.interceptors.headers.RequestHeadersProviders;

public class CollectedResponseHeadersProvider implements ResponseHeaderProvider {

    @Autowired
    @Qualifier("upHeaders")
    private ObjectProvider<RequestHeadersProviders.RequestHeadersStorage> upHeadersProvider;

    @Autowired
    private HeadersPropagationConfig config;

    @Override
    public List<String> getHeaderNames() {
        RequestHeadersProviders.RequestHeadersStorage s = upHeadersProvider.getIfAvailable();
        if (s != null) {
            return s.getHeaderList().stream().map(Header::getName).filter(name -> config.getUpPattern().matcher(name).matches()).distinct().collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getHeaderValues(String headerName) {
        RequestHeadersProviders.RequestHeadersStorage s = upHeadersProvider.getIfAvailable();
        if (s != null) {
            return s.getHeaderList().stream().filter(h-> StringUtils.equalsIgnoreCase(headerName, h.getName())).map(Header::getValue).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
