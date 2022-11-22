package io.github.springboot.httpclient.core.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.springframework.beans.factory.ObjectProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpRequestExecutorChain extends HttpRequestExecutor {

	private List<ChainableHttpRequestExecutor> allExecutors;
	private Stack<ChainableHttpRequestExecutor> currentExecutors;

	public HttpRequestExecutorChain(ObjectProvider<ChainableHttpRequestExecutor> executors) {
		this(executors.orderedStream().collect(Collectors.toList()));
	}

	public HttpRequestExecutorChain(List<ChainableHttpRequestExecutor> executors) {
		this(executors, HttpRequestExecutor.DEFAULT_WAIT_FOR_CONTINUE);

	}

	public HttpRequestExecutorChain(List<ChainableHttpRequestExecutor> executors, int waitForContinue) {
		super(waitForContinue);

		this.allExecutors = new ArrayList<>(executors);
		this.currentExecutors = new Stack<ChainableHttpRequestExecutor>();
		Collections.reverse(executors);
		this.currentExecutors.addAll(executors);
	}

	@Override
	public HttpResponse execute(HttpRequest request, HttpClientConnection conn, HttpContext context)
			throws IOException, HttpException {
		HttpRequestExecutorChain executorChain = new HttpRequestExecutorChain(allExecutors);
		return executorChain.doExecute(request, conn, context);
	}

	public HttpResponse doExecute(HttpRequest request, HttpClientConnection conn, HttpContext context)
			throws IOException, HttpException {
		if (!currentExecutors.isEmpty()) {
			ChainableHttpRequestExecutor next = currentExecutors.pop();
			try {
				log.debug("before {} doExecute()", next.getClass());
				return next.doExecute(request, conn, context, this);
			} finally {
				log.debug("after {} doExecute()", next.getClass());
			}
		} else {
			log.debug("before {} execute()", HttpRequestExecutor.class);
			try {
				return super.execute(request, conn, context);
			} finally {
				log.debug("after {} execute()", HttpRequestExecutor.class);
			}
		}
	}

}
