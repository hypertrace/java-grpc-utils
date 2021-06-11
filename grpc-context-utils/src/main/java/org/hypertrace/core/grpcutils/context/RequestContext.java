package org.hypertrace.core.grpcutils.context;

import io.grpc.Context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

  public Set<String> getRoles() {
    return getJwt().map(Jwt::getRoles).orElse(Collections.emptySet());
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

  public <T> ContextualKey<T> buildContextualKey(T data) {
    return new DefaultContextualKey<>(this, data);
  }

  public ContextualKey<Void> buildContextualKey() {
    return new DefaultContextualKey<>(this, null);
  }
}
