package org.hypertrace.core.grpcutils.client;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static io.grpc.Metadata.BINARY_BYTE_MARSHALLER;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;
import java.util.Map;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RequestContextAsCreds extends CallCredentials {
  private static Logger LOGGER = LoggerFactory.getLogger(RequestContextAsCreds.class);

  @Override
  public void thisUsesUnstableApi() {}

  /**
   * Adds the request headers in the request context to a Metadata object to be propagated in the
   * client call. All the headers are propagated so if any headers should be exempted from
   * propagation, that should be handled separately before this call is made.
   *
   * @param applier
   * @param requestContext
   */
  protected void applyRequestContext(MetadataApplier applier, RequestContext requestContext) {
    Metadata metadata = new Metadata();
    if (requestContext != null) {
      for (Map.Entry<String, String> entry : requestContext.getAll().entrySet()) {
        // Exclude null headers
        if (entry.getValue() != null) {
          String key = entry.getKey();
          if (key.toLowerCase().endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
            metadata.put(
                Metadata.Key.of(entry.getKey(), BINARY_BYTE_MARSHALLER),
                entry.getValue().getBytes());
          } else {
            metadata.put(
                Metadata.Key.of(entry.getKey(), ASCII_STRING_MARSHALLER), entry.getValue());
          }
        }
      }
    }
    applier.apply(metadata);
  }

  protected void applyFailure(MetadataApplier applier, Throwable e) {
    String msg = "An exception when obtaining RequestContext";
    LOGGER.error(msg, e);
    applier.fail(Status.UNAUTHENTICATED.withDescription(msg).withCause(e));
  }
}
