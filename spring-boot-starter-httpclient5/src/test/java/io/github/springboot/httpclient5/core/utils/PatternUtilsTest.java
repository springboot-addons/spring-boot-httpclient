package io.github.springboot.httpclient5.core.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.junit.jupiter.api.Test;

class PatternUtilsTest {

	@Test
	void shouldMatchWholeMethodAndUri() {
		assertTrue(PatternUtils.matches("GET http://example.com/a", "GET http://example\\.com/.*"));
		assertTrue(PatternUtils.matches("POST https://example.com/a", ".* https://example\\.com/.*"));
	}

	@Test
	void shouldNotMatchAnotherMethod() {
		assertFalse(PatternUtils.matches("POST http://example.com/a", "GET http://example\\.com/.*"));
	}

	@Test
	void shouldRequireAFullMatch() {
		assertFalse(PatternUtils.matches("GET http://example.com/a", "GET http://example\\.com"));
	}

	@Test
	void shouldMatchOnePattern() {
		assertTrue(PatternUtils.matchesOne("X-TEST-KEY", List.of("X-OTHER", "X-TEST-.*")));
	}

	@Test
	void shouldNotMatchAnyPattern() {
		assertFalse(PatternUtils.matchesOne("X-TEST-KEY", List.of("X-OTHER", "X-FOO-.*")));
		assertFalse(PatternUtils.matchesOne("X-TEST-KEY", List.of()));
	}

	@Test
	void shouldRejectInvalidPattern() {
		assertThrows(PatternSyntaxException.class, () -> PatternUtils.matches("any", "["));
	}
}
