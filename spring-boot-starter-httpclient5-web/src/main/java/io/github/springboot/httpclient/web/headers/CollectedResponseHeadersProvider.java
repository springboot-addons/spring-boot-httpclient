package io.github.springboot.httpclient.web.headers;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.Header;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.HeadersPropagationProperties;
import io.github.springboot.httpclient5.core.utils.PatternUtils;

public class CollectedResponseHeadersProvider implements ResponseHeaderProvider {

    @Autowired
    @Qualifier("upHeaders")
    private ObjectProvider<RequestHeadersProviders.RequestHeadersStorage> upHeadersProvider;

    @Autowired
    private HttpClient5Config config;

    @Override
    public List<String> getHeaderNames(String method, String uri) {
        RequestHeadersProviders.RequestHeadersStorage s = upHeadersProvider.getIfAvailable();
        HeadersPropagationProperties headerConfig = config.getRequestConfigProperties(method, uri).getHeadersPropagation() ;
        if (s != null) {
            return s.getHeaderList().stream().map(Header::getName).filter(name -> PatternUtils.matches(name, headerConfig.getUp())).distinct().collect(Collectors.toList());
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
