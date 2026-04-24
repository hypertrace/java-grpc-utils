package org.hypertrace.core.grpcutils.client;

import static java.util.stream.Collectors.toUnmodifiableList;

import io.grpc.Status;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class GrpcRetryPolicy {
  private static final String MAX_ATTEMPTS = "maxAttempts";
  private static final String INITIAL_BACKOFF = "initialBackoff";
  private static final String MAX_BACKOFF = "maxBackoff";
  private static final String BACKOFF_MULTIPLIER = "backoffMultiplier";
  private static final String RETRYABLE_STATUS_CODES = "retryableStatusCodes";

  int maxAttempts;
  Duration initialBackoff;
  Duration maxBackoff;
  double backoffMultiplier;
  @Singular List<Status.Code> retryableStatusCodes;

  Map<String, Object> toMap() {
    return Map.of(
        MAX_ATTEMPTS,
        (double) maxAttempts,
        INITIAL_BACKOFF,
        initialBackoff.toMillis() / 1000.0 + "s",
        MAX_BACKOFF,
        maxBackoff.toMillis() / 1000.0 + "s",
        BACKOFF_MULTIPLIER,
        backoffMultiplier,
        RETRYABLE_STATUS_CODES,
        retryableStatusCodes.stream().map(Enum::name).collect(toUnmodifiableList()));
  }
}
