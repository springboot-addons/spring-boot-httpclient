package io.github.springboot.httpclient.auth.cas;

import java.net.URL;
import java.util.List;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.apache.http.client.CookieStore;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.jasig.cas.client.util.AssertionHolder;
import org.jasig.cas.client.validation.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import io.github.springboot.httpclient.constants.HttpClientConstants;
import io.github.springboot.httpclient.utils.HostUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnClass(name = "org.jasig.cas.client.util.AssertionHolder")
@ConditionalOnWebApplication
public class CasAuthenticator {

  @Autowired(required = false)
  private Provider<CookieStore> cookieStore;

  @Autowired
  @Lazy
  private Executor httpExecutor;

  public void authCas(final HttpRequest request, String authenticationEndPoint) throws Exception {
    Assertion assertion = AssertionHolder.getAssertion();
    if (assertion == null) {
      final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null) {
        final CasAuthenticationToken token = (CasAuthenticationToken) authentication;
        assertion = token.getAssertion();
      }
    }
    if (assertion == null) {
      throw new IllegalStateException("No cas assertion found for CAS proxy ticket authentification");
    }

    String realAuthenticationEndPoint = authenticationEndPoint;
    final RequestLine requestLine = request.getRequestLine();
    final String requestUri = requestLine.getUri();

    final URL url = new URL(requestUri);
    final String rootPath = HostUtils.getRootPath(url);

    if (StringUtils.isBlank(realAuthenticationEndPoint)) {
      realAuthenticationEndPoint = HostUtils.getBaseUrl(url);
      if (StringUtils.isNotBlank(rootPath)) {
        realAuthenticationEndPoint += rootPath + HttpClientConstants.SLASH;
      }
      realAuthenticationEndPoint += HttpClientConstants.AUTH_LOGIN_PATH;
    }

    if (StringUtils.startsWithIgnoreCase(requestUri, realAuthenticationEndPoint)) {
      log.trace("Authentication request on URL ({}); no need to authenticate itself this request", request);
    } else {
      final String lastPath = StringUtils.substringAfterLast(url.getPath(), HttpClientConstants.SLASH);
      if (StringUtils.startsWithIgnoreCase(lastPath, HttpClientConstants.PUBLIC_PATH)) {
        log.debug("This URL ({}) is public, bypass CAS Authentication...", request);
      } else {
        log.debug("This URL ({}) is CAS protected, using Proxy Ticket mechanism to authenticate user", request);
        final String sessionId = hasSessionOn(url, rootPath);

        if (StringUtils.isBlank(sessionId)) {
          log.debug("Using CAS Authentication for url : {} on endpoint {}", request, realAuthenticationEndPoint);
          final String pt = assertion.getPrincipal().getProxyTicketFor(realAuthenticationEndPoint);
          if (StringUtils.isNotBlank(pt)) {
            log.debug("Using PT : {} for {}", pt, realAuthenticationEndPoint);
            final URIBuilder builder = new URIBuilder(realAuthenticationEndPoint);
            builder.addParameter(HttpClientConstants.TICKET_PARAMETER, pt);
            final String authenticationUrl = builder.build().toString();
            final String authenticationResponse = httpExecutor.execute(Request.Get(authenticationUrl)).returnContent()
                .asString();
            log.debug("PRE-Authenticating on URL : {}; gives : {}", authenticationUrl, authenticationResponse);
          } else {
            log.info("ProxyTicket for {} (CAS protected URL) is NULL, unable to authenticate through endpoint {}",
                request, realAuthenticationEndPoint);
          }
        } else {
          log.debug(
              "Already authenticated on CAS protected URL ({}), no need to authenticate again, current sessionID : {}",
              request, sessionId);
        }
      }
    }
  }

  private String hasSessionOn(URL url, String rootPath) {
    final CookieStore store = cookieStore.get();
    if (store == null) {
      return null;
    }

    String jSessionId = null;
    final List<Cookie> cookies = store.getCookies();
    if (cookies != null && !cookies.isEmpty()) {
      for (final Cookie cookie : cookies) {
        if (StringUtils.equalsIgnoreCase(cookie.getName(), HttpClientConstants.JESSION_ID_PARAMETER_NAME)
            && StringUtils.equalsIgnoreCase(cookie.getDomain(), url.getHost())
            && StringUtils.equalsIgnoreCase(cookie.getPath(), rootPath)) {
          jSessionId = cookie.getValue();
          break;
        }
      }
    }
    return jSessionId;
  }
}
