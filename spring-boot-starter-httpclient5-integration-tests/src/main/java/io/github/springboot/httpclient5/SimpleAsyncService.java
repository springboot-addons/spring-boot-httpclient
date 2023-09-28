package io.github.springboot.httpclient5;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.Future;

import org.apache.hc.client5.http.fluent.Executor;
import org.apache.hc.client5.http.fluent.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

@Service
public class SimpleAsyncService {

	private String base64ClientCredentials = new String(Base64.getEncoder().encode("admin:admin".getBytes()));

	@Autowired
	private Executor httpClient;

	@Async
	public Future<String> doIt() throws IOException {
		return new AsyncResult<String>(httpClient.execute(Request.get("http://localhost:8282/httpclient/back/header")
				.addHeader("Authorization", "Basic " + base64ClientCredentials)).returnContent().asString());

	}

	@Async
	public Future<String> doItAgain() throws IOException {
		return new AsyncResult<String>(httpClient.execute(Request.get("http://localhost:8282/httpclient/back/headeragain")
				.addHeader("Authorization", "Basic " + base64ClientCredentials)).returnContent().asString());
	}

}
