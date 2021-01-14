package org.hypertrace.core.grpcutils.client;

import io.grpc.Context;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.hypertrace.core.grpcutils.context.RequestContextConstants;

/**
 * Utility class which has methods to easily read and propagate headers from GRPC request context.
 */
public class GrpcClientRequestContextUtil {
  /**
   * Utility method to execute the given {@link Callable} in the context of given tenant by
   * setting the tenant id in the request headers.
   *
   * @throws RuntimeException if there was an issue in executing the request.
   */
  public static <V> V executeInTenantContext(String tenantId, Callable<V> c) {
    return executeWithHeadersContext(Map.of(RequestContextConstants.TENANT_ID_HEADER_KEY, tenantId), c);
  }

  /**
   * Same as the method above but this takes a Runnable instead of Callable. Useful when using async grpc client stubs.
   *
   * @param tenantId
   * @param r
   */
  public static void executeInTenantContext(String tenantId, Runnable r) {
    executeWithHeadersContext(Map.of(RequestContextConstants.TENANT_ID_HEADER_KEY, tenantId), r);
  }

  /**
   * Utility method to execute the given {@link Callable} in the context of given request headers.
   * The headers usually include tenant id and other distributed tracing related headers.
   *
   * @throws RuntimeException if there was an issue in executing the request.
   */
  public static <V> V executeWithHeadersContext(@Nonnull Map<String, String> headers, @Nonnull Callable<V> c) {
    RequestContext requestContext = new RequestContext();
    headers.forEach(requestContext::add);

    try {
      return Context.current().withValue(RequestContext.CURRENT, requestContext).call(c);
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException)e;
      }
      throw new RuntimeException(e);
    }
  }

  /**
   * Same as the method above but this takes a Runnable instead of Callable. Useful when using async grpc client stubs.
   *
   * @param headers
   * @param r
   */
  public static void executeWithHeadersContext(@Nonnull Map<String, String> headers, @Nonnull Runnable r) {
    RequestContext requestContext = new RequestContext();
    headers.forEach(requestContext::add);

    Context.current().withValue(RequestContext.CURRENT, requestContext).run(r);
  }
}
