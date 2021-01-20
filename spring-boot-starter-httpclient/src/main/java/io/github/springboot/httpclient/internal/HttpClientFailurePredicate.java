package io.github.springboot.httpclient.internal;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import javax.net.ssl.SSLHandshakeException;

public class HttpClientFailurePredicate implements Predicate<Throwable> {
  @Override
  public boolean test(Throwable e) {
    if (e instanceof UndeclaredThrowableException || e instanceof InvocationTargetException
        || e instanceof ExecutionException) {
      return test(e.getCause());
    } else {
      if (e instanceof UnknownHostException || e instanceof NoRouteToHostException
          || e instanceof SSLHandshakeException) {
        return true;
      } else {
        return e instanceof IOException;
      }
    }
  }
}