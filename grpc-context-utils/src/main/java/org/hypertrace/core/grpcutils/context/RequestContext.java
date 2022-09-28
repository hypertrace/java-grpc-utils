package org.hypertrace.core.grpcutils.context;

import static org.hypertrace.core.grpcutils.context.RequestContextConstants.CACHE_MEANINGFUL_HEADERS;
import static org.hypertrace.core.grpcutils.context.RequestContextConstants.TENANT_ID_HEADER_KEY;

import com.google.common.collect.Maps;
import io.grpc.Context;
import io.grpc.Metadata;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;

/**
 * Context of the GRPC request that should be carried and can made available to the services so that
 * the service can use them. We use this to propagate headers across services.
 */
public class RequestContext {
  public static final Context.Key<RequestContext> CURRENT = Context.key("request_context");

  public static RequestContext forTenantId(String tenantId) {
    RequestContext requestContext = new RequestContext();
    requestContext.add(RequestContextConstants.TENANT_ID_HEADER_KEY, tenantId);
    requestContext.add(RequestContextConstants.REQUEST_ID_HEADER_KEY, UUID.randomUUID().toString());
    return requestContext;
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
                requestContext.add(k, value);
              }
            });

    return requestContext;
  }

  private final Map<String, String> headers = new HashMap<>();
  private final JwtParser jwtParser = new JwtParser();

  /** Reads tenant id from this RequestContext based on the tenant id http header and returns it. */
  public Optional<String> getTenantId() {
    return get(RequestContextConstants.TENANT_ID_HEADER_KEY);
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
    return this.get(RequestContextConstants.REQUEST_ID_HEADER_KEY);
  }

  private Optional<Jwt> getJwt() {
    return get(RequestContextConstants.AUTHORIZATION_HEADER).flatMap(jwtParser::fromAuthHeader);
  }

  /** Method to read all GRPC request headers from this RequestContext. */
  public Map<String, String> getRequestHeaders() {
    return getAll();
  }

  public void add(String headerKey, String headerValue) {
    this.headers.put(headerKey, headerValue);
  }

  public Optional<String> get(String headerKey) {
    return Optional.ofNullable(this.headers.get(headerKey));
  }

  public Map<String, String> getAll() {
    return Map.copyOf(headers);
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

  private Map<String, String> getHeadersOtherThanAuth() {
    return Maps.filterKeys(
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
}
