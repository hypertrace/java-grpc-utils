package org.hypertrace.core.grpcutils.server;

import static org.hypertrace.core.grpcutils.context.RequestContextConstants.REQUEST_ID_HEADER_KEY;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.core.grpcutils.context.FastUUIDGenerator;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.slf4j.MDC;

@Slf4j
public final class RequestContextLoggingServerInterceptor implements ServerInterceptor {

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> serverCall,
      Metadata metadata,
      ServerCallHandler<ReqT, RespT> serverCallHandler) {
    RequestContext currentContext =
        Optional.ofNullable(RequestContext.CURRENT.get())
            .orElseGet(() -> RequestContext.fromMetadata(metadata));
    Optional<String> opRequestId = currentContext.getHeaderValue(REQUEST_ID_HEADER_KEY);
    if (opRequestId.isEmpty()) {
      opRequestId = Optional.of(FastUUIDGenerator.randomUUID().toString());
    }
    final String requestId = opRequestId.get();
    ServerCall.Listener<ReqT> listener =
        Contexts.interceptCall(
            Context.current().withValue(RequestContext.CURRENT, currentContext),
            serverCall,
            metadata,
            serverCallHandler);
    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {

      @Override
      public void onCancel() {
        try {
          MDC.clear();
        } catch (Exception e) {
          log.error("Error while clearing request context details from MDC params", e);
        }
        super.onCancel();
      }

      @Override
      public void onComplete() {
        try {
          MDC.clear();
        } catch (Exception e) {
          log.error("Error while clearing request context details from MDC params", e);
        }
        super.onComplete();
      }

      @Override
      public void onMessage(ReqT message) {
        try {
          MDC.put(REQUEST_ID_HEADER_KEY, requestId);
        } catch (Exception e) {
          log.error("Error while setting request context details in MDC params", e);
        }
        super.onMessage(message);
      }
    };
  }
}
