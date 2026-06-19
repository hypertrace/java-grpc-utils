package org.hypertrace.ratelimiter.grpcutils;

import org.hypertrace.ratelimiter.grpcutils.bucket4j.Bucket4jRateLimiterFactory;

public final class RateLimiterFactoryProvider {

  private RateLimiterFactoryProvider() {
    // Prevent instantiation
  }

  public static Bucket4jRateLimiterFactory bucket4j() {
    return new Bucket4jRateLimiterFactory();
  }
}
