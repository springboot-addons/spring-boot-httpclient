package io.github.springboot.httpclient5.core.configure;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLException;

import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.concurrent.CancellableDependency;
import org.apache.hc.core5.http.ConnectionClosedException;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.RequestNotExecutedException;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Args;
import org.apache.hc.core5.util.TimeValue;

import io.github.springboot.httpclient5.core.config.HttpClient5Config;
import io.github.springboot.httpclient5.core.config.model.RetryConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of the {@link HttpRequestRetryStrategy} interface.
 *
 * @since 5.0
 */
@Slf4j
public class ConfigurableHttpRequestRetryStrategy implements HttpRequestRetryStrategy {

    private static final String RETRY_INTERVAL = "RETRY_INTERVAL";
	private static final String MAX_RETRIES = "MAX_RETRIES";

    /**
     * Derived {@code IOExceptions} which shall not be retried
     */
    private final Set<Class<? extends IOException>> nonRetriableIOExceptionClasses;

    /**
     * HTTP status codes which shall be retried
     */
    private final Set<Integer> retriableCodes;

	private HttpClient5Config config;

    public ConfigurableHttpRequestRetryStrategy(
    		final HttpClient5Config config,
            final Collection<Class<? extends IOException>> clazzes,
            final Collection<Integer> codes) {
        this.nonRetriableIOExceptionClasses = new HashSet<>(clazzes);
        this.retriableCodes = new HashSet<>(codes);
		this.config = config;

    }

    /**
     * Create the HTTP request retry strategy using the following list of
     * non-retriable I/O exception classes:<br>
     * <ul>
     * <li>InterruptedIOException</li>
     * <li>UnknownHostException</li>
     * <li>ConnectException</li>
     * <li>ConnectionClosedException</li>
     * <li>NoRouteToHostException</li>
     * <li>SSLException</li>
     * </ul>
     *
     * and retriable HTTP status codes:<br>
     * <ul>
     * <li>SC_TOO_MANY_REQUESTS (429)</li>
     * <li>SC_SERVICE_UNAVAILABLE (503)</li>
     * </ul>
     *
     * @param defaultMaxRetries how many times to retry; 0 means no retries
     * @param defaultRetryInterval the default retry interval between
     * subsequent retries if the {@code Retry-After} header is not set
     * or invalid.
     */
    public ConfigurableHttpRequestRetryStrategy(
            final HttpClient5Config config) {
        this(config, 
        		Arrays.asList(
                        InterruptedIOException.class,
                        UnknownHostException.class,
                        ConnectException.class,
                        ConnectionClosedException.class,
                        NoRouteToHostException.class,
                        SSLException.class),
                Arrays.asList(
                        HttpStatus.SC_TOO_MANY_REQUESTS,
                        HttpStatus.SC_SERVICE_UNAVAILABLE));
    }

    @Override
    public boolean retryRequest(
            final HttpRequest request,
            final IOException exception,
            final int execCount,
            final HttpContext context) {
        Args.notNull(request, "request");
        Args.notNull(exception, "exception");
        
        if (execCount > this.getMaxRetries(request, context)) {
            // Do not retry if over max retries
            return false;
        }
        if (this.nonRetriableIOExceptionClasses.contains(exception.getClass())) {
            return false;
        } else {
            if (exception instanceof RequestNotExecutedException) {
                log.debug("retry {} of {}", execCount, request.getRequestUri()) ;
                return true;
            }
            for (final Class<? extends IOException> rejectException : this.nonRetriableIOExceptionClasses) {
                if (rejectException.isInstance(exception)) {
                    return false;
                }
            }
        }
        if (request instanceof CancellableDependency && ((CancellableDependency) request).isCancelled()) {
            return false;
        }

        // Retry if the request is considered idempotent
        boolean handleAsIdempotent = handleAsIdempotent(request);
        if (handleAsIdempotent) {
            log.debug("retry {} of {}", execCount, request.getRequestUri()) ;
        }
        
		return handleAsIdempotent;
    }

    @Override
    public boolean retryRequest(
            final HttpResponse response,
            final int execCount,
            final HttpContext context) {
        Args.notNull(response, "response");
        Object val = context.getAttribute(MAX_RETRIES);
        Integer maxRetries ;
        HttpRequest request = (HttpRequest) context.getAttribute(HttpClientContext.HTTP_REQUEST) ;
        if (val != null) {
        	maxRetries = (Integer) val;
        }
        else {
        	maxRetries = getMaxRetries(request, context) ;
        }
        boolean retryable = execCount <= maxRetries && retriableCodes.contains(response.getCode());
        if (retryable) {
            try {
				log.debug("retry {} of {} {}", execCount, request.getMethod(), request.getUri()) ;
			} catch (URISyntaxException ignore) {
			}
        }
        
		return retryable;
    }

    @Override
    public TimeValue getRetryInterval(
            final HttpResponse response,
            final int execCount,
            final HttpContext context) {
        Args.notNull(response, "response");

        final Header header = response.getFirstHeader(HttpHeaders.RETRY_AFTER);
        TimeValue retryAfter = null;
        if (header != null) {
            final String value = header.getValue();
            try {
                retryAfter = TimeValue.ofSeconds(Long.parseLong(value));
            } catch (final NumberFormatException ignore) {
                final Date retryAfterDate = DateUtils.parseDate(value);
                if (retryAfterDate != null) {
                    retryAfter =
                            TimeValue.ofMilliseconds(retryAfterDate.getTime() - System.currentTimeMillis());
                }
            }

            if (TimeValue.isPositive(retryAfter)) {
                return retryAfter;
            }
        }
        TimeValue retryInterval = (TimeValue) context.getAttribute(RETRY_INTERVAL) ;
        return retryInterval;
    }

    protected boolean handleAsIdempotent(final HttpRequest request) {
        return Method.isIdempotent(request.getMethod());
    }

    @SneakyThrows
	public int getMaxRetries(HttpRequest request, HttpContext context) {
		URI uri = request.getUri() ;
		final String method = request.getMethod();
		
		RetryConfig conf = config.getRequestConfigProperties(method, uri.toString()).getRetryConfig() ;
		
		Integer maxRetries = conf.getMaxRetries() == null ? 0 : conf.getMaxRetries() ;
		context.setAttribute(MAX_RETRIES, maxRetries);
		context.setAttribute(RETRY_INTERVAL, conf.getRetryInterval());
		return maxRetries ;
		
	}
}
