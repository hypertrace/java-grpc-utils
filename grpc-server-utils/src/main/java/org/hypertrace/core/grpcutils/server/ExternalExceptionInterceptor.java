package org.hypertrace.core.grpcutils.server;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static org.hypertrace.core.grpcutils.context.ContextualStatusExceptionBuilder.from;

import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.util.Optional;
import java.util.UUID;
import org.hypertrace.core.grpcutils.context.ContextualExceptionDetails;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.hypertrace.core.grpcutils.context.RequestContextConstants;

/**
 * This interceptor can be used at the edge to scrub any sensitive information such as an exception
 * cause or metadata off the context before propagating it
 */
public class ExternalExceptionInterceptor implements ServerInterceptor {

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
    ServerCall<ReqT, RespT> wrappedCall = new ExceptionWrappedServerCall<>(call, headers);
    return next.startCall(wrappedCall, headers);
  }

  private class ExceptionWrappedServerCall<ReqT, RespT>
      extends SimpleForwardingServerCall<ReqT, RespT> {
    private final Metadata headers;

    private ExceptionWrappedServerCall(ServerCall<ReqT, RespT> delegate, Metadata headers) {
      super(delegate);
      this.headers = headers;
    }

    @Override
    public void close(Status status, Metadata trailers) {
      Optional<ContextualExceptionDetails> details =
          resolveContextDetails(status, headers, trailers);
      String requestId =
          details
              .flatMap(ContextualExceptionDetails::getRequestContext)
              .flatMap(RequestContext::getRequestId)
              .orElseGet(ExternalExceptionInterceptor.this::generateDefaultRequestId);
      String message =
          details
              .flatMap(ContextualExceptionDetails::getExternalMessage)
              .orElseGet(ExternalExceptionInterceptor.this::getDefaultErrorMessage);
      Status externalStatus = buildExternalStatus(status, requestId, message);
      Metadata externalTrailers = buildExternalTrailers(trailers, requestId);
      super.close(externalStatus, externalTrailers);
    }

    /** Remove sensitive information from status before sending back to client. */
    private Optional<ContextualExceptionDetails> resolveContextDetails(
        Status status, Metadata headers, Metadata trailers) {
      // Preference to the returned trailers then thread local value and finally calling headers
      return ContextualExceptionDetails.fromMetadata(trailers)
          .or(() -> Optional.of(from(status, RequestContext.CURRENT.get()).getDetails()))
          .filter(
              details ->
                  details.getRequestContext().flatMap(RequestContext::getRequestId).isPresent())
          .or(() -> ContextualExceptionDetails.fromMetadata(headers));
    }
  }

  protected String generateDefaultRequestId() {
    return UUID.randomUUID().toString();
  }

  protected String getDefaultErrorMessage() {
    return "Error";
  }

  protected Status buildExternalStatus(Status status, String requestId, String message) {
    if (Status.OK.equals(status)) {
      return status;
    }

    return Status.fromCode(status.getCode())
        .withDescription(
            String.format("Request with id: %s failed with message: %s", requestId, message));
  }

  /** For now, only propagate request ID */
  protected Metadata buildExternalTrailers(Metadata receivedTrailers, String requestId) {
    Metadata externalTrailers = new Metadata();
    externalTrailers.put(
        Metadata.Key.of(RequestContextConstants.REQUEST_ID_HEADER_KEY, ASCII_STRING_MARSHALLER),
        requestId);
    return externalTrailers;
  }
}
