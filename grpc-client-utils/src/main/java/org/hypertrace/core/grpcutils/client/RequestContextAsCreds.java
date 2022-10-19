package org.hypertrace.core.grpcutils.client;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static io.grpc.Metadata.BINARY_BYTE_MARSHALLER;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.hypertrace.core.grpcutils.context.RequestContext.RequestContextHeader;
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
      for (RequestContextHeader header : requestContext.getAllHeaders()) {
        // Exclude null headers
        if (header.getValue() != null) {
          String key = header.getName();
          if (key.toLowerCase().endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
            metadata.put(
                Metadata.Key.of(header.getName(), BINARY_BYTE_MARSHALLER),
                header.getValue().getBytes());
          } else {
            metadata.put(
                Metadata.Key.of(header.getName(), ASCII_STRING_MARSHALLER), header.getValue());
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
