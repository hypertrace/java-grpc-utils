package org.hypertrace.ratelimiter.grpcutils;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hypertrace.core.grpcutils.context.RequestContext;

public class RateLimiterInterceptor implements ServerInterceptor {

  private final List<RateLimiterConfiguration>
      rateLimitConfigs; // Provided via config or dynamic update
  private final RateLimiterFactory rateLimiterFactory;

  public RateLimiterInterceptor(
      List<RateLimiterConfiguration> rateLimitConfigs, RateLimiterFactory factory) {
    this.rateLimitConfigs = rateLimitConfigs;
    this.rateLimiterFactory = factory;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

    String method = call.getMethodDescriptor().getFullMethodName();

    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(
        next.startCall(call, headers)) {
      @Override
      public void onMessage(ReqT message) {
        RequestContext requestContext = RequestContext.fromMetadata(headers);
        for (RateLimiterConfiguration config : rateLimitConfigs) {
          if (!config.getMethod().equals(method)) continue;

          Map<String, String> attributes =
              config.getAttributeExtractor().apply(requestContext, message);

          if (!matches(config.getMatchAttributes(), attributes)) continue;
          int tokens = config.getTokenCostFunction().apply(requestContext, message);
          String key = buildRateLimitKey(method, config.getMatchAttributes(), attributes);
          boolean allowed =
              rateLimiterFactory
                  .getRateLimiter(config)
                  .tryAcquire(key, tokens, config.getRateLimit());
          if (!allowed) {
            call.close(Status.RESOURCE_EXHAUSTED.withDescription("Rate limit exceeded"), headers);
            return;
          }
        }
        super.onMessage(message);
      }
    };
  }

  private boolean matches(Map<String, String> match, Map<String, String> actual) {
    return match.entrySet().stream()
        .allMatch(e -> Objects.equals(actual.get(e.getKey()), e.getValue()));
  }

  private String buildRateLimitKey(
      String method, Map<String, String> keys, Map<String, String> attrs) {
    return method
        + "::"
        + keys.keySet().stream()
            .map(k -> attrs.getOrDefault(k, "null"))
            .collect(Collectors.joining(":"));
  }
}
