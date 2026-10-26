package org.hypertrace.circuitbreaker.grpcutils;

import io.grpc.Status;
import java.time.Duration;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CircuitBreakerThresholds {
  // Percentage of failures to trigger OPEN state
  @Builder.Default float failureRateThreshold = 50f;
  // Percentage of slow calls to trigger OPEN state
  @Builder.Default float slowCallRateThreshold = 50f;
  // Define what a "slow" call is
  @Builder.Default Duration slowCallDurationThreshold = Duration.ofSeconds(2);
  // Number of calls to consider in the sliding window
  @Builder.Default SlidingWindowType slidingWindowType = SlidingWindowType.TIME_BASED;
  @Builder.Default int slidingWindowSize = 60;
  // Time before retrying after OPEN state
  @Builder.Default Duration waitDurationInOpenState = Duration.ofSeconds(60);
  // Minimum calls before evaluating failure rate
  @Builder.Default int minimumNumberOfCalls = 10;
  // Calls allowed in HALF_OPEN state before deciding to
  // CLOSE or OPEN again
  @Builder.Default int permittedNumberOfCallsInHalfOpenState = 5;
  @Builder.Default boolean enabled = true;

  @Builder.Default
  Set<Status.Code> failureStatusCodes =
      Set.of(
          Status.Code.DEADLINE_EXCEEDED, Status.Code.UNAVAILABLE, Status.Code.RESOURCE_EXHAUSTED);

  public enum SlidingWindowType {
    COUNT_BASED,
    TIME_BASED
  }
}
