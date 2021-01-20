package io.github.springboot.httpclient.utils;

import java.net.URI;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;

import io.github.springboot.httpclient.constants.HttpClientConstants;

/**
 * Classe utilitaire contenant des fonctionnalités de récupération du host/port.
 *
 */
public final class HostUtils {

  public static final String HTTPS = "https";

  public static final int HTTPS_PORT = 443;

  public static final String HTTP = "http";

  public static final int HTTP_PORT = 80;

  // private static final String PROTOCOL_SEPARATOR = "://";
  //
  private static ThreadLocal<StringBuilder> sbThreadLocal = ThreadLocal.withInitial(() -> new StringBuilder()) ;

  private HostUtils() {
  }

  public static String getBaseUrl(URL request) {
    final String scheme = request.getProtocol() + "://";
    final String serverName = request.getHost();
    final String serverPort = request.getPort() == HTTP_PORT || request.getPort() == HTTPS_PORT
        || request.getPort() == -1 ? StringUtils.EMPTY : ":" + request.getPort();
    return scheme + serverName + serverPort;
  }

  public static String getRootPath(URL url) {
    final String path = url.getPath();
    return HttpClientConstants.SLASH + StringUtils.substringBetween(path, HttpClientConstants.SLASH);
  }

  /**
   * Normalisation du nom du host.
   *
   * @param host
   * @return
   */
  public static String normalizeHostName(final String host) {
    return host.contains(":") ? host : getSb().append(host).append(":").append(HTTP_PORT).toString();
  }

  /**
   * Récupération du port.
   *
   * @param host
   * @return
   */
  public static int getPort(final String host) {
    final String hostAndPort = HostUtils.normalizeHostName(host);
    final int port = Integer.parseInt(hostAndPort.substring(hostAndPort.indexOf(':') + 1));
    return port;
  }

  /**
   * Recuperation du port en fonction du protocol.
   *
   * @param uri
   * @return
   */
  public static int getPort(final URI uri) {
    return getPort(uri.getPort(), uri.getScheme());
  }

  /**
   * Recuperation du port en fonction du protocol.
   *
   * @param port
   * @param schemeName
   * @return
   */
  public static int getPort(final int port, final String schemeName) {
    int portResult = port;
    if (portResult == -1) {
      if (schemeName.equals(HTTP)) {
        portResult = HTTP_PORT;
      } else if (schemeName.equals(HTTPS)) {
        portResult = HTTPS_PORT;
      }
    }
    return portResult;
  }

  private static StringBuilder getSb() {
    final StringBuilder sb = sbThreadLocal.get();
    sb.setLength(0);
    return sb;
  }

}
