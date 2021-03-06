package org.hypertrace.core.grpcutils.context;

import io.grpc.Metadata;
import java.util.Set;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

/**
 * GRPC request context constants used to propagate the tenantId, authorization token, tracing headers etc
 * in the platform services.
 */
public class RequestContextConstants {
  public static final String TENANT_ID_HEADER_KEY = "x-tenant-id";

  public static final Metadata.Key<String> TENANT_ID_METADATA_KEY =
      Metadata.Key.of(TENANT_ID_HEADER_KEY, ASCII_STRING_MARSHALLER);

  public static final String AUTHORIZATION_HEADER = "authorization";

  /**
   * The values in this set are looked up with case insensitivity.
   */
  public static final Set<String> HEADER_PREFIXES_TO_BE_PROPAGATED =
      Set.of(TENANT_ID_HEADER_KEY, "X-B3-", "grpc-trace-bin",
          "traceparent", "tracestate", AUTHORIZATION_HEADER);
}
