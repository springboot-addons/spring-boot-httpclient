package io.github.springboot.httpclient.web.headers;

import java.util.List;

/**
 * Interface for response headers providers.
 */
public interface ResponseHeaderProvider {

    /**
     * Retrieves the list of header names provided (generated) by this provider
     * @return names of the headers
     */
    public List<String> getHeaderNames(String method, String uri) ;

    /**
     * Retrieves the values of the header.
     * @param headerName name of the header
     * @return values of the header, empty list if header is not provided by this provider
     */
    List<String> getHeaderValues(String headerName);

}
