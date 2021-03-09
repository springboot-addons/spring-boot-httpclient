package io.github.springboot.httpclient.auth.cas;

import java.net.URL;
import java.util.List;

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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import io.github.springboot.httpclient.core.constants.HttpClientConstants;
import io.github.springboot.httpclient.core.utils.HostUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CasAuthenticator {

	@Autowired
	private ObjectProvider<CookieStore> cookieStoreProvider;

	@Autowired
	@Lazy
	private Executor httpExecutor;

	public void authCas(HttpRequest request, String authenticationEndPoint) throws Exception {
		Assertion assertion = AssertionHolder.getAssertion();
		if (assertion == null) {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication != null) {
				CasAuthenticationToken token = (CasAuthenticationToken) authentication;
				assertion = token.getAssertion();
			}
		}
		if (assertion == null) {
			throw new IllegalStateException("No cas assertion found for CAS proxy ticket authentification");
		}

		String realAuthenticationEndPoint = authenticationEndPoint;
		RequestLine requestLine = request.getRequestLine();
		String requestUri = requestLine.getUri();

		URL url = new URL(requestUri);
		String rootPath = HostUtils.getRootPath(url);

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
			String lastPath = StringUtils.substringAfterLast(url.getPath(), HttpClientConstants.SLASH);
			if (StringUtils.startsWithIgnoreCase(lastPath, HttpClientConstants.PUBLIC_PATH)) {
				log.debug("This URL ({}) is public, bypass CAS Authentication...", request);
			} else {
				log.debug("This URL ({}) is CAS protected, using Proxy Ticket mechanism to authenticate user", request);
				String sessionId = hasSessionOn(url, rootPath);

				if (StringUtils.isBlank(sessionId)) {
					log.debug("Using CAS Authentication for url : {} on endpoint {}", request,
							realAuthenticationEndPoint);
					String pt = assertion.getPrincipal().getProxyTicketFor(realAuthenticationEndPoint);
					if (StringUtils.isNotBlank(pt)) {
						log.debug("Using PT : {} for {}", pt, realAuthenticationEndPoint);
						URIBuilder builder = new URIBuilder(realAuthenticationEndPoint);
						builder.addParameter(HttpClientConstants.TICKET_PARAMETER, pt);
						String authenticationUrl = builder.build().toString();
						String authenticationResponse = httpExecutor.execute(Request.Get(authenticationUrl))
								.returnContent().asString();
						log.debug("PRE-Authenticating on URL : {}; gives : {}", authenticationUrl,
								authenticationResponse);
					} else {
						log.info(
								"ProxyTicket for {} (CAS protected URL) is NULL, unable to authenticate through endpoint {}",
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
		CookieStore store = cookieStoreProvider.getIfAvailable();
		if (store == null) {
			return null;
		}

		String jSessionId = null;
		List<Cookie> cookies = store.getCookies();
		if (cookies != null && !cookies.isEmpty()) {
			for (Cookie cookie : cookies) {
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
