package org.springframework.boot.httpclient.constants;

/**
 * Classe d'interface des constantes HTTP
 */
public interface HttpClientConstants {

  String WILDCARD_PREFIX_CERTIFICATE = "*.";
  String KERBEROS_PARAM_PROPERTY = "java.security.krb5.conf";
  String API_MANAGER_HEADER_API_KEY = "X-Cda-Lr-Api-Key";
  String X_ALFRESCO_REMOTE_USER = "X-Alfresco-Remote-User";
  String X_FRONT_WEB = "X-Front-ID";
  int SC_TOO_MANY_REQUESTS = 429;

  String POST_METHOD = "POST";
  String GET_METHOD = "GET";
  String HEAD_METHOD = "HEAD";
  String DELETE_METHOD = "DELETE";
  String PUT_METHOD = "PUT";
  String TRACE_METHOD = "TRACE";
  String CONNECT_METHOD = "CONNECT";
  String OPTIONS_METHOD = "OPTIONS";
  String PATCH_METHOD = "PATCH";

  String REQUEST_CONTENT_STREAM_KEY = "REQUEST_CONTENT_AS_STREAM";
  String REQUEST_CONTENT_LENGTH_KEY = "ENTITY_CONTENT_LENGTH";
  String MULTIPART_FORM_DATA = "multipart/form-data";
  String REQUEST_CONTENT_MIMETYPE_KEY = "ENTITY_CONTENT_TYPE";

  String USERNAME_KEY = "USERNAME";
  String PASSWORD_KEY = "PASSWORD";
  String NTLM_DOMAIN_KEY = "NTLM_DOMAIN";
  String AUTH_TYPE_KEY = "AUTH_TYPE";
  String USERINFO_KEY = "_USERINFO_";
  String BASIC_AUTHENTIFICATION_SCHEME = "Basic";
  String CAS_AUTHENTIFICATION_SCHEME = "CAS";
  String NTLM_AUTHENTIFICATION_SCHEME = "NTLM";
  String DEFAULT_AUTHENTIFICATION_SCHEME = BASIC_AUTHENTIFICATION_SCHEME;

  String PARAM_JSON = "JSON";
  String PARAM_RAW = "RAW";

  String URL_CONTEXT = "__URL_CONTEXT";
  String DEFAULT_ENCODING = "UTF-8";
  String CAS_TGT_PARAMETER = "TGC";
  String PEER_CERTIFICATES = "PEER_CERTIFICATES";
  String TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";
  String TRUSTSTORE = "javax.net.ssl.trustStore";
  String KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword";
  String KEYSTORE = "javax.net.ssl.keyStore";
  String TRUSTSTORE_TYPE = "javax.net.ssl.trustStoreType";
  String KEYSTORE_TYPE = "javax.net.ssl.keyStoreType";
  String KEYSTORE_DEFAULT_TYPE = "JKS";
  String JESSION_ID_PARAMETER_NAME = "jsessionid";
  String TICKET_PARAMETER = "ticket";
  String PUBLIC_PATH = "public";
  String SLASH = "/";
  String AUTH_LOGIN_PATH = "auth/login";
  String CONTENT_TYPE = "Content-Type";
  String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
  String CONTENT_LENGTH = "Content-Length";
  String ACCEPT = "Accept";
  String AUTHORIZATION = "Authorization";

  String CONTENT_ENCODING = "Content-Encoding";
  String ENCRYPTION = "Encryption";
  String CRYPTO_KEY = "Crypto-Key";
  String TTL = "TTL";

  String ACCEPT_ENCODING = "Accept-Encoding";
  String ACCEPT_ENCODING_GZIP = "gzip,deflate,br";
  String RANGE = "Range";

  String ACCEPT_LANGUAGE = "Accept-Language";
  String CONTENT_ENCODING_GZIP = "gzip";
  String CONTENT_ENCODING_BROTLI = "br";
  String CONTENT_ENCODING_DEFLATE = "deflate";

  String LOCATION = "Location";
  String CONTENT_RANGE = "Content-Range";
  String CONTENT_DISPOSITION = "Content-Disposition";

  String USER_AGENT = "user-agent";

  int TYPE_HTTP_REDIRECT = 3;

  int TYPE_HTTP_FORBIDDEN = 4;

  int TYPE_HTTP_OK = 2;

  int GRANDEUR_HTTP = 100;

  int HTTP_KO = 500;

  String MIME_APPLICATION_OCTET_STREAM = "application/octet-stream";
}
