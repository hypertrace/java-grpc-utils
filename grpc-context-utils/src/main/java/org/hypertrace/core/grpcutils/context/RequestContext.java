package org.hypertrace.core.grpcutils.context;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static java.util.Objects.requireNonNull;
import static org.hypertrace.core.grpcutils.context.RequestContextConstants.CACHE_MEANINGFUL_HEADERS;
import static org.hypertrace.core.grpcutils.context.RequestContextConstants.TENANT_ID_HEADER_KEY;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Value;

/**
 * Context of the GRPC request that should be carried and can made available to the services so that
 * the service can use them. We use this to propagate headers across services.
 */
public class RequestContext {
  public static final Context.Key<RequestContext> CURRENT = Context.key("request_context");

  public static RequestContext forTenantId(String tenantId) {
    return new RequestContext()
        .put(RequestContextConstants.TENANT_ID_HEADER_KEY, tenantId)
        .put(
            RequestContextConstants.REQUEST_ID_HEADER_KEY,
            UuidGenerator.generateFastRandomUUID().toString());
  }

  public static RequestContext fromMetadata(Metadata metadata) {
    RequestContext requestContext = new RequestContext();

    // Go over all the headers and copy the allowed headers to the RequestContext.
    metadata.keys().stream()
        .filter(
            k ->
                RequestContextConstants.HEADER_PREFIXES_TO_BE_PROPAGATED.stream()
                    .anyMatch(prefix -> k.toLowerCase().startsWith(prefix.toLowerCase())))
        .forEach(
            k -> {
              String value;
              // check if key ends with binary suffix
              if (k.toLowerCase().endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                byte[] bytes = metadata.get(Metadata.Key.of(k, Metadata.BINARY_BYTE_MARSHALLER));
                value = new String(bytes, StandardCharsets.UTF_8);
              } else {
                value = metadata.get(Metadata.Key.of(k, Metadata.ASCII_STRING_MARSHALLER));
              }
              // The value could be null or empty for some keys so validate that.
              if (value != null && !value.isEmpty()) {
                requestContext.put(k, value);
              }
            });

    return requestContext;
  }

  private final ListMultimap<String, RequestContextHeader> headers =
      MultimapBuilder.linkedHashKeys().linkedListValues().build();
  private final JwtParser jwtParser = new JwtParser();

  /** Reads tenant id from this RequestContext based on the tenant id http header and returns it. */
  public Optional<String> getTenantId() {
    return getHeaderValue(RequestContextConstants.TENANT_ID_HEADER_KEY);
  }

  public Optional<String> getUserId() {
    return getJwt().flatMap(Jwt::getUserId);
  }

  public Optional<String> getName() {
    return getJwt().flatMap(Jwt::getName);
  }

  public Optional<String> getPictureUrl() {
    return getJwt().flatMap(Jwt::getPictureUrl);
  }

  public Optional<String> getEmail() {
    return getJwt().flatMap(Jwt::getEmail);
  }

  @Deprecated
  public List<String> getRoles(String rolesClaim) {
    return getClaim(rolesClaim)
        .flatMap(claim -> claim.asList(String.class))
        .orElse(Collections.emptyList());
  }

  public Optional<JwtClaim> getClaim(String claimName) {
    return getJwt().flatMap(jwt -> jwt.getClaim(claimName));
  }

  public Optional<String> getRequestId() {
    return this.getHeaderValue(RequestContextConstants.REQUEST_ID_HEADER_KEY);
  }

  private Optional<Jwt> getJwt() {
    return this.getHeaderValue(RequestContextConstants.AUTHORIZATION_HEADER)
        .flatMap(jwtParser::fromAuthHeader);
  }

  /**
   * This is retained for backwards compatibility, but is based on the incorrect assumption that a
   * header only can have one value. For the updated API, please use {@link #getAllHeaders()}}
   */
  @Deprecated
  public Map<String, String> getRequestHeaders() {
    return getAll();
  }

  /**
   * This is retained for backwards compatibility. It previously was implemented with the assumption
   * of a single value per header, so overwrote on addition. This is maintained by first removing
   * any values for the given header name, then adding
   *
   * @param headerName
   * @param headerValue
   */
  @Deprecated
  public void add(String headerName, String headerValue) {
    this.removeHeader(headerName);
    this.put(headerName, headerValue);
  }

  /** Prefer {@link #getHeaderValue} */
  @Deprecated
  public Optional<String> get(String headerName) {
    return this.getHeaderValue(headerName);
  }

  /**
   * This is retained for backwards compatibility, but is based on the incorrect assumption that a
   * header only can have one value. For the updated API, please use {@link #getAllHeaders()} ()}
   */
  @Deprecated
  public Map<String, String> getAll() {
    return this.getHeaderNames().stream()
        .flatMap(key -> this.headers.get(key).stream().findFirst().stream())
        .collect(
            Collectors.toUnmodifiableMap(
                RequestContextHeader::getName, RequestContextHeader::getValue));
  }

  /**
   * Adds the provided header name and header value. Duplicates are allowed. For fluency, the
   * current instance is returned.
   */
  public RequestContext put(String headerName, String headerValue) {
    this.headers.put(
        this.normalizeHeaderName(requireNonNull(headerName)),
        new RequestContextHeader(headerName, headerValue));
    return this;
  }

  /** Returns all header names in normalized form (case not preserved) */
  @Nonnull
  public Set<String> getHeaderNames() {
    return Set.copyOf(this.headers.keySet());
  }

  /**
   * Removes and returns all headers matching the provided name. Header names are case insensitive.
   * Returns an empty list if no headers have been removed.
   */
  @Nonnull
  public List<String> removeHeader(String name) {
    return this.headers.removeAll(this.normalizeHeaderName(name)).stream()
        .map(RequestContextHeader::getValue)
        .collect(Collectors.toUnmodifiableList());
  }

  /** Returns all header values matching the provided header name, case insensitively. */
  @Nonnull
  public List<String> getAllHeaderValues(String key) {
    return this.headers.get(this.normalizeHeaderName(key)).stream()
        .map(RequestContextHeader::getValue)
        .collect(Collectors.toUnmodifiableList());
  }

  /**
   * Returns all header name-value pairs, with the original case preserved. Multiple headers with
   * the same name, and potentially same value may be returned.
   */
  @Nonnull
  public List<RequestContextHeader> getAllHeaders() {
    return List.copyOf(this.headers.values());
  }

  /**
   * Gets the first header value specified for the provided name (case insensitive), or empty if no
   * match.
   */
  @Nonnull
  public Optional<String> getHeaderValue(String key) {
    return this.headers.get(this.normalizeHeaderName(key)).stream()
        .map(RequestContextHeader::getValue)
        .findFirst();
  }

  public <V> V call(@Nonnull Callable<V> callable) {
    try {
      return Context.current().withValue(RequestContext.CURRENT, this).call(callable);
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException(e);
    }
  }

  public void run(@Nonnull Runnable runnable) {
    Context.current().withValue(RequestContext.CURRENT, this).run(runnable);
  }

  /**
   * @deprecated - Use {@link #buildInternalContextualKey(Object)} ()} or {@link
   *     #buildUserContextualKey(Object)} instead, as appropriate. This delegates to {@link
   *     #buildUserContextualKey(Object)} to match its original implementation for compatibility.
   */
  @Deprecated
  public <T> ContextualKey<T> buildContextualKey(T data) {
    return this.buildUserContextualKey(data);
  }

  /**
   * @deprecated - Use {@link #buildInternalContextualKey()} or {@link #buildUserContextualKey()}
   *     instead, as appropriate. This delegates to {@link #buildUserContextualKey()} to match its
   *     original implementation for compatibility.
   */
  @Deprecated
  public ContextualKey<Void> buildContextualKey() {
    return this.buildUserContextualKey();
  }

  /** This returns a cache key based on this user request context. */
  public ContextualKey<Void> buildUserContextualKey() {
    return new DefaultContextualKey<>(this, null, CACHE_MEANINGFUL_HEADERS);
  }

  /**
   * An extension of {@link #buildUserContextualKey()} that also includes {@code data} as part of
   * the cache key.
   */
  public <T> ContextualKey<T> buildUserContextualKey(T data) {
    return new DefaultContextualKey<>(this, data, CACHE_MEANINGFUL_HEADERS);
  }

  /**
   * This returns a cache key based on this internal request context. It should only be used if this
   * request is not on behalf of a specific user - either one initiated by the platform or by the
   * agent.
   */
  public ContextualKey<Void> buildInternalContextualKey() {
    return new DefaultContextualKey<>(this, null, List.of(TENANT_ID_HEADER_KEY));
  }

  /**
   * An extension of {@link @buildInternalContextualKey()} that also includes {@code data} as part
   * of the cache key.
   */
  public <T> ContextualKey<T> buildInternalContextualKey(T data) {
    return new DefaultContextualKey<>(this, data, List.of(TENANT_ID_HEADER_KEY));
  }

  /** Converts the request context into metadata to be used as trailers */
  public Metadata buildTrailers() {
    Metadata trailers = new Metadata();
    // For now, the only context item to use as a trailer is the request id
    this.getRequestId()
        .ifPresent(
            requestId ->
                trailers.put(
                    Key.of(RequestContextConstants.REQUEST_ID_HEADER_KEY, ASCII_STRING_MARSHALLER),
                    requestId));
    return trailers;
  }

  private String normalizeHeaderName(@Nonnull String headerName) {
    return headerName.toLowerCase();
  }

  private Multimap<String, RequestContextHeader> getHeadersOtherThanAuth() {
    return Multimaps.filterKeys(
        headers, key -> !key.equals(RequestContextConstants.AUTHORIZATION_HEADER));
  }

  @Override
  public String toString() {
    final String emptyValue = "{}";
    return "RequestContext{"
        + "headers="
        + getHeadersOtherThanAuth()
        + ", jwt="
        + getJwt().map(Jwt::toString).orElse(emptyValue)
        + '}';
  }

  @Value
  public static class RequestContextHeader {
    String name;
    String value;
  }
}
