package io.github.springboot.httpclient5.actuator;
//package io.github.springboot.httpclient5.actuator;
//
//import static com.codahale.metrics.MetricRegistry.name;
//
//import java.net.URISyntaxException;
//
//import org.apache.http.Header;
//import org.apache.http.HttpRequest;
//import org.apache.http.RequestLine;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.utils.URIBuilder;
//
//import com.codahale.metrics.httpclient.HttpClientMetricNameStrategy;
//
//public class HttpClientMetricNameStrategies {
//
//	public static final HttpClientMetricNameStrategy METHOD_ONLY = (name, request) -> name(HttpClient.class, name,
//			methodNameString(request));
//
//	public static final HttpClientMetricNameStrategy HOST_AND_METHOD = (name, request) -> {
//		String host = null;
//		final Header hostHeader = request.getFirstHeader("Host");
//		if (hostHeader != null) {
//			host = hostHeader.getValue();
//		}
//		return name(HttpClient.class, name, host, methodNameString(request));
//	};
//
//	public static final HttpClientMetricNameStrategy QUERYLESS_URL_AND_METHOD = (name, request) -> {
//		try {
//			final RequestLine requestLine = request.getRequestLine();
//			final URIBuilder url = new URIBuilder(requestLine.getUri());
//			return name(HttpClient.class, name, url.removeQuery().build().toString(), methodNameString(request));
//		} catch (final URISyntaxException e) {
//			throw new IllegalArgumentException(e);
//		}
//	};
//
//	private static String methodNameString(HttpRequest request) {
//		return request.getRequestLine().getMethod().toLowerCase() + "-requests";
//	}
//}