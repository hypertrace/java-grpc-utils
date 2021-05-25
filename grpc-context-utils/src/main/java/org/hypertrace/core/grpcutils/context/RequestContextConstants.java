package org.hypertrace.core.grpcutils.context;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import io.grpc.Metadata;
import java.util.Set;

/**
 * GRPC request context constants used to propagate the tenantId, authorization token, tracing
 * headers etc in the platform services.
 */
public class RequestContextConstants {
  public static final String TENANT_ID_HEADER_KEY = "x-tenant-id";

  public static final Metadata.Key<String> TENANT_ID_METADATA_KEY =
      Metadata.Key.of(TENANT_ID_HEADER_KEY, ASCII_STRING_MARSHALLER);

  public static final String AUTHORIZATION_HEADER = "authorization";

  /** The values in this set are looked up with case insensitivity. */
  public static final Set<String> HEADER_PREFIXES_TO_BE_PROPAGATED =
      Set.of(
          TENANT_ID_HEADER_KEY,
          "X-B3-",
          "grpc-trace-bin",
          "traceparent",
          "tracestate",
          AUTHORIZATION_HEADER);

  /**
   * These headers may affect returned results and should be accounted for in any cached remote
   * results
   */
  static final Set<String> CACHE_MEANINGFUL_HEADERS =
      Set.of(TENANT_ID_HEADER_KEY, AUTHORIZATION_HEADER);
}
