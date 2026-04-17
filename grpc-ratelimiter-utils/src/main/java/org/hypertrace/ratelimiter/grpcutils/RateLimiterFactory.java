package org.hypertrace.ratelimiter.grpcutils;

public interface RateLimiterFactory {
  RateLimiter getRateLimiter(RateLimiterConfiguration rateLimiterConfiguration);
}
