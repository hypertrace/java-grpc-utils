package org.hypertrace.core.grpcutils.context;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

class DefaultContextualKey<T> implements ContextualKey<T> {
  private final RequestContext context;
  private final T data;
  private final Multimap<String, String> cacheableContextHeaders;

  DefaultContextualKey(RequestContext context, T data, Collection<String> cacheableHeaderNames) {
    this.context = context;
    this.data = data;
    this.cacheableContextHeaders = this.extractCacheableHeaders(context, cacheableHeaderNames);
  }

  @Override
  public RequestContext getContext() {
    return this.context;
  }

  @Override
  public T getData() {
    return this.data;
  }

  @Override
  public <R> R callInContext(Function<T, R> function) {
    return this.context.call(() -> function.apply(this.getData()));
  }

  @Override
  public <R> R callInContext(Supplier<R> supplier) {
    return this.context.call(supplier::get);
  }

  @Override
  public void runInContext(Consumer<T> consumer) {
    this.context.run(() -> consumer.accept(this.getData()));
  }

  @Override
  public void runInContext(Runnable runnable) {
    this.context.run(runnable);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DefaultContextualKey<?> that = (DefaultContextualKey<?>) o;
    return Objects.equals(getData(), that.getData())
        && cacheableContextHeaders.equals(that.cacheableContextHeaders);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getData(), cacheableContextHeaders);
  }

  @Override
  public String toString() {
    return "DefaultContextualKey{"
        + "data="
        + data
        + ", cacheableContextHeaders="
        + cacheableContextHeaders
        + '}';
  }

  private Multimap<String, String> extractCacheableHeaders(
      RequestContext requestContext, Collection<String> cacheableHeaderNames) {
    return cacheableHeaderNames.stream()
        .collect(
            ImmutableListMultimap.flatteningToImmutableListMultimap(
                Function.identity(),
                headerName -> requestContext.getAllHeaderValues(headerName).stream()));
  }
}
