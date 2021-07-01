package io.github.springboot.httpclient.web.headers;

import java.util.Enumeration;

/**
 * Interface for request headers collectors.
 */
public interface RequestHeaderCollector {

    /**
     * Indicates if header is supported by the collector
     * @param headerName name of the header
     * @return true if header is supported (collected) by collector, false otherwise
     */
    boolean supports(String headerName);

    /**
     * Handle a header
     * @param headerName name of the header
     * @param headerValue value of the header
     */
    void handle(String headerName, String headerValue);

    /**
     * Handle a header
     * @param headerName name of the header
     * @param headerValues values of the header
     */
    void handle(String headerName, Enumeration<String> headerValues);

}
