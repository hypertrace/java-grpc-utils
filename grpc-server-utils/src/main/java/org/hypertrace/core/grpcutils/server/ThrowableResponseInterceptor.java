package org.hypertrace.core.grpcutils.server;

import static io.grpc.Status.Code.UNKNOWN;
import static java.util.Objects.isNull;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.util.Optional;
import org.hypertrace.core.grpcutils.context.RequestContext;

/** Server Interceptor that decorates the error response status before closing the call */
public class ThrowableResponseInterceptor implements ServerInterceptor {
  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
    ServerCall<ReqT, RespT> wrappedCall =
        new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
          @Override
          public void sendMessage(RespT message) {
            super.sendMessage(message);
          }

          @Override
          public void close(Status status, Metadata trailers) {
            if (status.getCode() == UNKNOWN
                && status.getDescription() == null
                && status.getCause() != null) {
              status =
                  Status.INTERNAL
                      .withDescription(status.getCause().getMessage())
                      .withCause(status.getCause());
            }
            super.close(
                status,
                collectAndMergeMetadata(RequestContext.fromMetadata(headers), trailers, status));
          }
        };

    return next.startCall(wrappedCall, headers);
  }

  private Metadata collectAndMergeMetadata(
      RequestContext requestContext, Metadata originalTrailers, Status status) {
    Metadata mergedTrailers = new Metadata();
    // We build with increasing precedence (since metadata.get() reads last value)
    mergedTrailers.merge(requestContext.buildTrailers());
    mergedTrailers.merge(collectAllTrailersFromCause(status));
    mergedTrailers.merge(originalTrailers);

    return mergedTrailers;
  }

  private Metadata collectAllTrailersFromCause(Status status) {
    // Two base cases - either no cause or an unknown cause
    Throwable cause = status.getCause();
    if (isNull(cause)) {
      return new Metadata();
    }
    Status statusFromCause = Status.fromThrowable(cause);
    if (statusFromCause.getCode() == UNKNOWN) {
      return new Metadata();
    }
    // Otherwise, we've found a status so collect any trailers from it and merge them on top of
    // any trailers we can find from descendents
    Metadata trailersFromCauseDescendents = this.collectAllTrailersFromCause(statusFromCause);
    Optional.ofNullable(Status.trailersFromThrowable(cause))
        .ifPresent(trailersFromCauseDescendents::merge);
    return trailersFromCauseDescendents;
  }
}
