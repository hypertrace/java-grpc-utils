package org.hypertrace.ratelimiter.grpcutils;

import java.util.Map;
import java.util.function.BiFunction;
import lombok.Builder;
import lombok.Value;
import org.hypertrace.core.grpcutils.context.RequestContext;

@Value
@Builder
public class RateLimiterConfiguration {
  boolean enabled;
  String method;
  // Attributes to match like tenant_id -> traceable
  Map<String, String> matchAttributes;

  // Extract attributes from gRPC request
  BiFunction<RequestContext, Object, Map<String, String>> attributeExtractor;

  // Token cost evaluator (can be static 1 or dynamic based on message)
  @Builder.Default BiFunction<RequestContext, Object, Integer> tokenCostFunction = (ctx, req) -> 1;
  RateLimit rateLimit;

  @Value
  @Builder
  public static class RateLimit {
    int tokens;
    int refreshPeriodSeconds;
  }
}
