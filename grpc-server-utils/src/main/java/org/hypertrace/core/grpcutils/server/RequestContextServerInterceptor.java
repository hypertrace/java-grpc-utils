package org.hypertrace.core.grpcutils.server;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import java.util.Optional;
import java.util.UUID;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.hypertrace.core.grpcutils.context.RequestContextConstants;

/**
 * Interceptor which intercepts the request headers to extract request context and sets it in the
 * context so that the server logic can use the context and they can be passed onto other downstream
 * services.
 */
public class RequestContextServerInterceptor implements ServerInterceptor {

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> serverCall,
      Metadata metadata,
      ServerCallHandler<ReqT, RespT> serverCallHandler) {
    RequestContext currentContext =
        Optional.ofNullable(RequestContext.CURRENT.get())
            .orElseGet(() -> RequestContext.fromMetadata(metadata));
    if (currentContext.getHeaderValue(RequestContextConstants.REQUEST_ID_HEADER_KEY).isEmpty()) {
      currentContext.put(
          RequestContextConstants.REQUEST_ID_HEADER_KEY, UUID.randomUUID().toString());
    }

    return Contexts.interceptCall(
        Context.current().withValue(RequestContext.CURRENT, currentContext),
        serverCall,
        metadata,
        serverCallHandler);
  }
}
