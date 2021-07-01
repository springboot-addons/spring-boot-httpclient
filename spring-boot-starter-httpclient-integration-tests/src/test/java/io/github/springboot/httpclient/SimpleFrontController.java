package io.github.springboot.httpclient;

import java.util.Base64;
import java.util.concurrent.Future;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.slf4j.Slf4j;

@Controller
@ConditionalOnWebApplication
@RequestMapping("/front")
@Slf4j
public class SimpleFrontController {
	private String base64ClientCredentials = new String(Base64.getEncoder().encode("admin:admin".getBytes()));

	@Autowired
	private Executor httpClient;

	@Autowired
	private SimpleAsyncService service;

	@GetMapping(path = "/header", produces = "application/json")
	public ResponseEntity<?> getHeader() {
		log.debug("*** SimpleFrontController.getHeader() invoked");
		String content;
		try {
			content = httpClient.execute(Request.Get("http://localhost:8282/httpclient/back/header")
					.addHeader("Authorization", "Basic " + base64ClientCredentials)).returnContent().asString();

			return ResponseEntity.ok(content);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@GetMapping(path = "/async-header", produces = "application/json")
	public ResponseEntity<?> getAsyncHeader() {
		log.debug("*** SimpleFrontController.getAsyncHeader() invoked");
		try {
			String content = service.doIt().get();
			return ResponseEntity.ok(content);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@GetMapping(path = "/multiasync-header", produces = "application/json")
	public ResponseEntity<?> getMultiAsyncHeader() {
		log.debug("*** SimpleFrontController.getMultiAsyncHeader() invoked");
		try {
			Future<String> s1 = service.doIt();
			Future<String> s2 = service.doItAgain();
			return ResponseEntity.ok(s1.get() + " " + s2.get());
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

}