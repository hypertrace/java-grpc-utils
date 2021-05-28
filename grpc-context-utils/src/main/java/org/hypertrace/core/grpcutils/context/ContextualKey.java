package org.hypertrace.core.grpcutils.context;

import java.util.function.Consumer;
import java.util.function.Function;

public interface ContextualKey<T> {
  RequestContext getContext();

  T getData();

  /**
   * Calls the function in the key's context and providing the key's data as an argument, returning
   * any result
   */
  <R> R callInContext(Function<T, R> function);

  /**
   * Calls the function in the key's context and providing the key's data as an argument, returning
   * no result
   */
  void runInContext(Consumer<T> consumer);
}
