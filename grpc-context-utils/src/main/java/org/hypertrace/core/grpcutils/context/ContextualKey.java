package org.hypertrace.core.grpcutils.context;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ContextualKey<T> {
  RequestContext getContext();

  T getData();

  /**
   * Calls the function in the key's context and providing the key's data as an argument, returning
   * any result
   */
  <R> R callInContext(Function<T, R> function);

  <R> R callInContext(Supplier<R> supplier);

  /**
   * Calls the function in the key's context and providing the key's data as an argument, returning
   * no result
   */
  void runInContext(Consumer<T> consumer);

  void runInContext(Runnable runnable);
}
