package org.hypertrace.core.grpcutils.server;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Server Interceptor that decorates the error response status before closing the call
 */
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
            if (status.getCode() == Status.Code.UNKNOWN
                && status.getDescription() == null
                && status.getCause() != null) {
              Throwable e = status.getCause();
              status = Status.INTERNAL
                  .withDescription(e.getMessage())
                  .augmentDescription(stacktraceToString(e));
            }
            super.close(status, trailers);
          }
        };

    return next.startCall(wrappedCall, headers);
  }

  private String stacktraceToString(Throwable e) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    e.printStackTrace(printWriter);
    return stringWriter.toString();
  }
}
