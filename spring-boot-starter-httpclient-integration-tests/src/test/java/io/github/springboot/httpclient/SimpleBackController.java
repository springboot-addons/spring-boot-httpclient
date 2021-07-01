package io.github.springboot.httpclient;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.slf4j.Slf4j;

@Controller
@ConditionalOnWebApplication
@RequestMapping("/back")
@Slf4j
public class SimpleBackController {
	@Autowired
	private HttpServletRequest request;

	@GetMapping(path = "/header", produces = "application/json")
	public ResponseEntity<?> getHeader() {
		log.debug("*** SimpleBackController.getHeader() invoked");
		if (!"srules".equals(request.getHeader("X-TEST-KEY"))) {

			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}
		return ResponseEntity.ok().header("X-TEST-RESPONSE", "yeah").body("ack");
	}

	@GetMapping(path = "/headeragain", produces = "application/json")
	public ResponseEntity<?> getHeaderAgain() {
		log.debug("*** SimpleBackController.getHeaderAgain() invoked");
		if (!"srules".equals(request.getHeader("X-TEST-KEY"))) {

			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}
		return ResponseEntity.ok().header("X-TEST-RESPONSE", "again").body("ack");
	}
}