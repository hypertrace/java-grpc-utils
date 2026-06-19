package org.hypertrace.ratelimiter.grpcutils.bucket4j;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.bucket4j.Bucket;
import io.grpc.Status;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import org.hypertrace.ratelimiter.grpcutils.RateLimiter;
import org.hypertrace.ratelimiter.grpcutils.RateLimiterConfiguration;
import org.hypertrace.ratelimiter.grpcutils.RateLimiterFactory;

public class Bucket4jRateLimiterFactory implements RateLimiterFactory {

  private final Cache<String, Bucket> limiterCache =
      CacheBuilder.newBuilder().maximumSize(10_000).build();

  @Override
  public RateLimiter getRateLimiter(RateLimiterConfiguration rule) {
    return (key, tokens, limit) -> {
      try {
        Bucket bucket = limiterCache.get(key, () -> createBucket(limit));
        return bucket.tryConsume(tokens);
      } catch (ExecutionException e) {
        throw Status.INTERNAL
            .withDescription("Failed to create rate limiter bucket for key: " + key)
            .withCause(e)
            .asRuntimeException();
      }
    };
  }

  private Bucket createBucket(RateLimiterConfiguration.RateLimit limit) {
    return Bucket.builder()
        .addLimit(
            bandwidth ->
                bandwidth
                    .capacity(limit.getTokens())
                    .refillGreedy(
                        limit.getTokens(), Duration.ofSeconds(limit.getRefreshPeriodSeconds())))
        .build();
  }
}
