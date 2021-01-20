package io.github.springboot.httpclient.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;

import io.github.springboot.httpclient.constants.ConfigurationConstants;
import io.github.springboot.httpclient.constants.HttpClientConstants;
import io.github.springboot.httpclient.internal.ProgressEntityWrapper;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gdespres
 *
 */
@Slf4j
public final class HttpClientUtils {

  public static final int TYPE_HTTP_REDIRECT = 3;

  public static final int TYPE_HTTP_FORBIDDEN = 4;

  public static final int TYPE_HTTP_OK = 2;

  public static final int GRANDEUR_HTTP = 100;

  public static final int HTTP_KO = 500;

  public static boolean isSuccess(int statusCode) {
    return statusCode / GRANDEUR_HTTP == TYPE_HTTP_OK;
  }

  private static final ConcurrentMap<Object, String> CACHED_TARGET_HOST = new ConcurrentHashMap<>();

  private static ThreadLocal<StringBuilder> sbThreadLocal = ThreadLocal.withInitial(() -> new StringBuilder()) ;


  // ========================================================================
  // CONSTRUCTEUR
  // ========================================================================

  private HttpClientUtils() {
  }

  // ========================================================================
  // METHODES PUBLIQUES
  // ========================================================================

  /**
   * Méthode permettant de recuperer un objet {@link URI} a partir d'objets
   * {@link HttpRequest} et {@link HttpContext}.
   *
   * @param httpRequest {@link HttpRequest}
   * @param httpContext {@link HttpContext}
   *
   * @return {@link URI}
   *
   * @throws IOException
   */
  public static URI getUri(HttpRequest httpRequest, HttpContext httpContext) throws IOException {

    URI uri = null;
    if (httpRequest instanceof HttpUriRequest) {
      uri = ((HttpUriRequest) httpRequest).getURI();
      if (uri.getHost() == null) {
        try {
          final Object targetHostKey = httpContext.getAttribute("http.target_host");
          String targetHost = CACHED_TARGET_HOST.get(targetHostKey);
          if (null == targetHost) {
            targetHost = targetHostKey.toString();
            CACHED_TARGET_HOST.put(targetHostKey, targetHost);
          }
          uri = new URI(getSb().append(targetHost).append(uri).toString());
        } catch (final URISyntaxException e) {
          throw new IOException(e.getMessage(), e);
        }
      }
    } else if (httpRequest instanceof BasicHttpRequest) {
      try {
        uri = new URI(((BasicHttpRequest) httpRequest).getRequestLine().getUri());
      } catch (final URISyntaxException e) {
        throw new IOException(e.getMessage(), e);
      }

    }
    return uri;
  }

  public static HttpHost getHttpHost(final String pUri) throws URISyntaxException {
    URI uri = new URI(pUri);
    final int port = HostUtils.getPort(uri);
    final String host = uri.getHost();
    if (port == HostUtils.HTTP_PORT) {
      return new HttpHost(host);
    } else {
      return new HttpHost(host, port);
    }
  }

  public static boolean isDisabledCookiePolicy(final String cookiePolicy) {
    return ConfigurationConstants.DISABLE_COOKIES_MANAGEMENT.equals(cookiePolicy);
  }

  private static StringBuilder getSb() {
    final StringBuilder sb = sbThreadLocal.get();
    sb.setLength(0);
    return sb;
  }

  public static Header[] toHeadersArray(Map<String, String> headersMap) {
    List<Header> headers = new ArrayList<Header>(headersMap.size());
    for (Entry<String, String> h : headersMap.entrySet()) {
      headers.add(new BasicHeader(h.getKey(), h.getValue()));
    }
    return headers.toArray(new Header[] {});
  }

  @Deprecated
  public static Request attachHttpEntity(Request req, final Map<String, String> headers,
      final Map<String, Object> content) throws IOException {

    boolean isMultiPart = false;
    if (headers != null) {
      for (final Entry<String, String> entry : headers.entrySet()) {
        if (HttpHeaders.CONTENT_TYPE.contains(entry.getKey()) && StringUtils.isNotEmpty(entry.getValue())
            && HttpClientConstants.MULTIPART_FORM_DATA.equals(entry.getValue())) {
          // La gestion du multipart dans le header est généré par la classe
          // MultipartEntity qui ajoute un boundary
          isMultiPart = true;
        } else {
          req.setHeader(entry.getKey(), entry.getValue());
        }
      }
    }
    return addContent(req, content, isMultiPart);
  }

  @SuppressWarnings("unchecked")
  private static Request addContent(Request request, final Map<String, Object> content, final boolean multiPart)
      throws IOException {
    ContentType contentType = ContentType.APPLICATION_OCTET_STREAM;
    if (content.containsKey(HttpClientConstants.REQUEST_CONTENT_MIMETYPE_KEY)) {
      contentType = ContentType.parse((String) content.get(HttpClientConstants.REQUEST_CONTENT_MIMETYPE_KEY));
      content.remove(HttpClientConstants.REQUEST_CONTENT_MIMETYPE_KEY);
    }
    ContentType effectiveContentType = contentType;
    final ContentType textDefaultContentType = ContentType.create(ContentType.TEXT_PLAIN.getMimeType(),
        Charset.forName(HttpClientConstants.DEFAULT_ENCODING));

    final Object cs = content.get(HttpClientConstants.REQUEST_CONTENT_STREAM_KEY);

    long contentLength = -1;
    if (content.containsKey(HttpClientConstants.REQUEST_CONTENT_LENGTH_KEY)) {
      contentLength = (long) content.get(HttpClientConstants.REQUEST_CONTENT_LENGTH_KEY);
    }

    ProgressEntityWrapper.ProgressListener pListener = new ProgressEntityWrapper.ProgressListener() {
      @Override
      public void progress(float percentage) {
        int percent = (int) percentage;
        if (percent > 0) {
          log.debug("{}%{}", percent, StringUtils.repeat("-", percent));
        }
        // boolean valid = Float.compare(percentage, 100) > 0;
      }
    };
    HttpEntity he = null;
    if (!multiPart && (cs != null)) {
      if (cs instanceof InputStream) {
        he = new ProgressEntityWrapper(new InputStreamEntity((InputStream) cs, contentLength, effectiveContentType),
            pListener);
      } else if (cs instanceof byte[]) {
        he = new ProgressEntityWrapper(new ByteArrayEntity((byte[]) cs, effectiveContentType), pListener);
      } else if (cs instanceof File) {
        he = new ProgressEntityWrapper(new FileEntity((File) cs, effectiveContentType), pListener);
      } else if (cs instanceof String) {
        he = new ProgressEntityWrapper(new StringEntity((String) cs, effectiveContentType), pListener);
      } else if (cs instanceof HashMap) {
        // TODO deprecated à supprimer dans une version majeur et éventuellement
        // faire évoluer le bloc ci-dessous (multipart).
        final HashMap<String, String> inputContent = (HashMap<String, String>) cs;
        final MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        final Set<String> idsFlux = inputContent.keySet();
        for (final String idFlux : idsFlux) {
          final ContentBody contentBody = new InputStreamBody(
              new ByteArrayInputStream(inputContent.get(idFlux).getBytes()), idFlux);
          multipartEntityBuilder.addPart(idFlux, contentBody);
        }
        he = new ProgressEntityWrapper(multipartEntityBuilder.build(), pListener);
      }
    } else 
        //if ((request. instanceof HttpPost) || (request instanceof HttpPut) || (request instanceof HttpPatch)
        //    || (request instanceof HttpDeleteWithBody)) {
        
        if (multiPart) {
      final MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
      multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
      multipartEntityBuilder.setCharset(Charset.forName(HttpClientConstants.DEFAULT_ENCODING));

      for (final Entry<String, Object> entry : content.entrySet()) {
        if ((entry.getKey() != null) && (entry.getValue() != null)) {
          if (entry.getValue() instanceof File) {
            final File inputContent = (File) entry.getValue();
//            final String fieldName = StringUtils.substringBeforeLast(inputContent.getName(), ".");
            multipartEntityBuilder.addBinaryBody(entry.getKey(), (File) entry.getValue(), contentType,
                inputContent.getName());
          } else if (entry.getValue() instanceof InputStream) {
            multipartEntityBuilder.addBinaryBody(entry.getKey(), (InputStream) entry.getValue(), effectiveContentType,
                entry.getKey());
          } else if (entry.getValue() instanceof byte[]) {
            multipartEntityBuilder.addBinaryBody(entry.getKey(), (byte[]) entry.getValue(), effectiveContentType,
                entry.getKey());
          } else {
            multipartEntityBuilder.addTextBody(entry.getKey(), (String) entry.getValue(), textDefaultContentType);
          }
        }
      }

      HttpEntity multipartEntity = multipartEntityBuilder.build();
      he = new ProgressEntityWrapper(multipartEntity, pListener);
    } else {

      final List<NameValuePair> formparams = new ArrayList<NameValuePair>();
      // gestion du param JSON (un seul param en entree) : pour client-AJ

      final Object paramRaw = content.get(HttpClientConstants.PARAM_RAW);
      if (null != paramRaw) {
        he = new ByteArrayEntity((byte[]) paramRaw);
      } else {
        final Object paramValueJson = content.get(HttpClientConstants.PARAM_JSON);
        if (paramValueJson != null) {
          he = new StringEntity(paramValueJson.toString(), ContentType.APPLICATION_JSON);
        } else { // le cas nominal
          for (final Entry<String, Object> entry : content.entrySet()) {
            if ((entry.getKey() != null) && (entry.getValue() != null)) {
              if (entry.getValue() instanceof String) {
                formparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
              }
            }
          }
          if (formparams != null && !formparams.isEmpty()) {
            he = new UrlEncodedFormEntity(formparams, HttpClientConstants.DEFAULT_ENCODING);
          }
        }
      }
    }

    if (he != null) {
      request.body(he);
    }

//    } else if (content.size() > 0) {
//      try {
//        BeanUtils.populate(request, content);
//      } catch (final Exception e) {
//        log.warn("Unable to set request content properties on this HttpMethod", e);
//      }
//    }

    return request;
  }

}
