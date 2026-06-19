package org.hypertrace.ratelimiter.grpcutils;

public interface RateLimiter {
  default boolean tryAcquire(String key, RateLimiterConfiguration.RateLimit rateLimit) {
    return tryAcquire(key, 1, rateLimit);
  } // default single token

  boolean tryAcquire(
      String key, int permits, RateLimiterConfiguration.RateLimit rateLimit); // new: batch tokens
}
