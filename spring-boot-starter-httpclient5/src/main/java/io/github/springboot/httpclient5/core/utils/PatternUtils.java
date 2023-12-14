package io.github.springboot.httpclient5.core.utils;
import java.util.List;
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

	public boolean matchesOne(String toTest, List<String> patterns) {
		boolean match = false ;
		for (String p : patterns) {
			match = matches(toTest, p) ;
			if (match) {
				break;
			}
		}
		return match ;
	}

}
