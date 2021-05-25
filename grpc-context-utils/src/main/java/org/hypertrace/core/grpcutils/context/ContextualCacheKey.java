package org.hypertrace.core.grpcutils.context;

import java.util.function.Consumer;
import java.util.function.Function;

public interface ContextualCacheKey<T> {
  RequestContext getContext();

  T getData();

  /**
   * Calls the provided function with the cached data as an argument in the cached context,
   * returning any result
   */
  <R> R callInContext(Function<T, R> function);

  /**
   * Calls the provided function with the cached data as an argument in the cached context. This is
   * a no result version of the callInContext api.
   */
  void runInContext(Consumer<T> consumer);
}
