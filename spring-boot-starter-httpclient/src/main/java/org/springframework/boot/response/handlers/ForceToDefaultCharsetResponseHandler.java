package org.springframework.boot.response.handlers;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.httpclient.utils.HttpClientUtils;

public class ForceToDefaultCharsetResponseHandler implements ResponseHandler<String> {

  @Override
  public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
    int status = response.getStatusLine().getStatusCode();
    if (HttpClientUtils.isSuccess(status)) {
        HttpEntity entity = response.getEntity();
        return entity != null ? EntityUtils.toString(entity, Charset.defaultCharset()) : null;
    } else {
        throw new ClientProtocolException("Unexpected response status: " + status);
    }
  }

}
