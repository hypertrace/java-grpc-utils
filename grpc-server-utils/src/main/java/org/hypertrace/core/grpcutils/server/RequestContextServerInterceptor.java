package org.hypertrace.core.grpcutils.server;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.hypertrace.core.grpcutils.context.RequestContext;

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
    RequestContext requestContext = RequestContext.fromMetadata(metadata);
    Context ctx = Context.current().withValue(RequestContext.CURRENT, requestContext);

    return Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler);
  }
}
