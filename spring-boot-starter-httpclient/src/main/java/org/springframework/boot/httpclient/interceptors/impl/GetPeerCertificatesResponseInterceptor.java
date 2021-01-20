package org.springframework.boot.httpclient.interceptors.impl;

import java.io.IOException;

import javax.net.ssl.SSLSession;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.springframework.boot.httpclient.constants.HttpClientConstants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetPeerCertificatesResponseInterceptor implements HttpResponseInterceptor {

    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
        final ManagedHttpClientConnection routedConnection = (ManagedHttpClientConnection) context
                .getAttribute(HttpCoreContext.HTTP_CONNECTION);
        final SSLSession sslSession = routedConnection.getSSLSession();
        log.info("Getting peer certificates");
        if (sslSession != null) {
            final java.security.cert.Certificate[] certificates = sslSession.getPeerCertificates();
            context.setAttribute(HttpClientConstants.PEER_CERTIFICATES, certificates);
        }
    }

}
