package io.github.springboot.httpclient5.core.utils;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PatternUtils {
	private static Map<String, Pattern> cache = new ConcurrentHashMap<>(); 
	
	public boolean matches(String toTest, String pattern) {
		return cache.computeIfAbsent(pattern, p -> Pattern.compile(p)).matcher(toTest).matches();
	}
}
