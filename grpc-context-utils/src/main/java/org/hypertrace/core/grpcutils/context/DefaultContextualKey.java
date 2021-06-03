package org.hypertrace.core.grpcutils.context;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class DefaultContextualKey<T> implements ContextualKey<T> {
  private final RequestContext context;
  private final T data;
  private final Map<String, String> meaningfulContextHeaders;

  DefaultContextualKey(RequestContext context, T data) {
    this.context = context;
    this.data = data;
    this.meaningfulContextHeaders = this.extractMeaningfulHeaders(context.getRequestHeaders());
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
        && meaningfulContextHeaders.equals(that.meaningfulContextHeaders);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getData(), meaningfulContextHeaders);
  }

  @Override
  public String toString() {
    return "DefaultContextualKey{"
        + "data="
        + data
        + ", meaningfulContextHeaders="
        + meaningfulContextHeaders
        + '}';
  }

  private Map<String, String> extractMeaningfulHeaders(Map<String, String> allHeaders) {
    return allHeaders.entrySet().stream()
        .filter(
            entry ->
                RequestContextConstants.CACHE_MEANINGFUL_HEADERS.contains(
                    entry.getKey().toLowerCase()))
        .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue));
  }
}
