package org.hypertrace.core.grpcutils.server;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import java.nio.charset.StandardCharsets;
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
    RequestContext requestContext = createRequestContextFromMetadata(metadata);
    Context ctx = Context.current().withValue(RequestContext.CURRENT, requestContext);

    return Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler);
  }

  /**
   * Add headers that our services would need internally or would need to propagate to downstream
   * services, from the metadata object into a RequestContext object. object.
   *
   * @param metadata
   * @return
   */
  RequestContext createRequestContextFromMetadata(Metadata metadata) {
    RequestContext requestContext = new RequestContext();

    // Go over all the headers and copy the allowed headers to the RequestContext.
    metadata.keys().stream()
        .filter(
            k ->
                RequestContextConstants.HEADER_PREFIXES_TO_BE_PROPAGATED.stream()
                    .anyMatch(prefix -> k.toLowerCase().startsWith(prefix.toLowerCase())))
        .forEach(
            k -> {
              String value;
              // check if key ends with binary suffix
              if (k.toLowerCase().endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                byte[] bytes = metadata.get(Metadata.Key.of(k, Metadata.BINARY_BYTE_MARSHALLER));
                value = new String(bytes, StandardCharsets.UTF_8);
              } else {
                value = metadata.get(Metadata.Key.of(k, Metadata.ASCII_STRING_MARSHALLER));
              }
              // The value could be null or empty for some keys so validate that.
              if (value != null && !value.isEmpty()) {
                requestContext.add(k, value);
              }
            });

    return requestContext;
  }
}
