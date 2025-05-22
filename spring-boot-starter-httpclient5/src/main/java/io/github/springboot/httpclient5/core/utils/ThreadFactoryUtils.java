package io.github.springboot.httpclient5.core.utils;

import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThreadFactoryUtils {
	private static ThreadFactory TF = null;
	private static boolean enabled = false ;

	public ThreadFactoryUtils(boolean virtualEnabled) {
		ThreadFactoryUtils.enabled = virtualEnabled ;
	}
	
	public static ThreadFactory getThreadFactory() {
		if (TF == null) {
			try {
				if (ThreadFactoryUtils.enabled) {
					TF = getVirtualThreadFactoryViaReflection();
					log.info("Using virtual threads for socle executors");
				}
				else {
					log.info("Using standard platform threads for socle executors");
					TF = Executors.defaultThreadFactory();
				}
			} catch (Exception e) {
				log.debug("Unable to invoke Thread.ofVirtual().factory()", e);
				log.info("Using standard platform threads for socle executors");
				TF = Executors.defaultThreadFactory();
			}
		}

		return TF;
	}
	
	protected static ThreadFactory getVirtualThreadFactoryViaReflection() throws Exception {
		Class<?> ofVirtualClass = Class.forName("java.lang.Thread$Builder$OfVirtual") ;
		Method method = Thread.class.getMethod("ofVirtual", new Class[0]);
		Object ov =  method.invoke(null, new Object[0]) ;
		
		Method factoryMethod = ofVirtualClass.getMethod("factory", new Class[0]) ;
		return (ThreadFactory) factoryMethod.invoke(ov, new Object[0]) ;
	}
}
