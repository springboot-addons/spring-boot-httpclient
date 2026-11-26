package io.github.springboot.httpclient5;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a disposable httpbin container shared by the tests of the JVM.
 * <p>
 * Docker must be available. The container is stopped by a shutdown hook.
 */
public final class HttpBinTestSupport {

	public static final String HTTPBIN_IMAGE = "mccutchen/go-httpbin:2.26.0";

	private static final int HTTP_PORT = 8080;

	private static final GenericContainer<?> HTTPBIN = new GenericContainer<>(DockerImageName.parse(HTTPBIN_IMAGE))
			.withExposedPorts(HTTP_PORT)
			.waitingFor(Wait.forHttp("/get").forStatusCode(200));

	static {
		HTTPBIN.start();
		Runtime.getRuntime().addShutdownHook(new Thread(HTTPBIN::stop, "httpbin-container-shutdown"));
	}

	private HttpBinTestSupport() {
	}

	public static String host() {
		return HTTPBIN.getHost();
	}

	public static int port() {
		return HTTPBIN.getMappedPort(HTTP_PORT);
	}

	public static String url() {
		return "http://" + host() + ":" + port();
	}
}
