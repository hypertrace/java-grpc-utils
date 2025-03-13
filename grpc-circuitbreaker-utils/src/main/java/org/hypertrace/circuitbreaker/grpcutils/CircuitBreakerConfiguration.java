package org.hypertrace.circuitbreaker.grpcutils;

import java.time.Duration;
import lombok.Builder;
import lombok.Setter;
import lombok.Value;

@Value
@Builder
@Setter
public class CircuitBreakerConfiguration {
  // Percentage of failures to trigger OPEN state
  float failureRateThreshold;
  // Percentage of slow calls to trigger OPEN state
  float slowCallRateThreshold;
  // Define what a "slow" call is
  Duration slowCallDurationThreshold;
  // Number of calls to consider in the sliding window
  SlidingWindowType slidingWindowType;
  int slidingWindowSize;
  // Time before retrying after OPEN state
  Duration waitDurationInOpenState;
  // Minimum calls before evaluating failure rate
  int minimumNumberOfCalls;
  // Calls allowed in HALF_OPEN state before deciding to
  // CLOSE or OPEN again
  int permittedNumberOfCallsInHalfOpenState;

  public enum SlidingWindowType {
    COUNT_BASED,
    TIME_BASED
  }
}
