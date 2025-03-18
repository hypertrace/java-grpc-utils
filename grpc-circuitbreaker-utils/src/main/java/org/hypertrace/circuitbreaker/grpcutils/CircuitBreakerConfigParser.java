package org.hypertrace.circuitbreaker.grpcutils;

import com.typesafe.config.Config;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CircuitBreakerConfigParser {

  // Percentage of failures to trigger OPEN state
  private static final String FAILURE_RATE_THRESHOLD = "failureRateThreshold";
  // Percentage of slow calls to trigger OPEN state
  private static final String SLOW_CALL_RATE_THRESHOLD = "slowCallRateThreshold";
  // Define what a "slow" call is
  private static final String SLOW_CALL_DURATION_THRESHOLD = "slowCallDurationThreshold";
  // Number of calls to consider in the sliding window
  private static final String SLIDING_WINDOW_SIZE = "slidingWindowSize";
  // Time before retrying after OPEN state
  private static final String WAIT_DURATION_IN_OPEN_STATE = "waitDurationInOpenState";
  // Minimum calls before evaluating failure rate
  private static final String MINIMUM_NUMBER_OF_CALLS = "minimumNumberOfCalls";
  // Calls allowed in HALF_OPEN state before deciding to
  // CLOSE or OPEN again
  private static final String PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE =
      "permittedNumberOfCallsInHalfOpenState";
  private static final String SLIDING_WINDOW_TYPE = "slidingWindowType";
  public static final String ENABLED = "enabled";
  public static final String DEFAULT_THRESHOLDS = "defaultThresholds";
  private static final Set<String> NON_THRESHOLD_KEYS = Set.of(ENABLED, DEFAULT_THRESHOLDS);

  public static <T> CircuitBreakerConfiguration.CircuitBreakerConfigurationBuilder<T> parseConfig(
      Config config) {
    CircuitBreakerConfiguration.CircuitBreakerConfigurationBuilder<T> builder =
        CircuitBreakerConfiguration.builder();
    if (config.hasPath(ENABLED)) {
      builder.enabled(config.getBoolean(ENABLED));
    }

    if (config.hasPath(DEFAULT_THRESHOLDS)) {
      builder.defaultThresholds(
          buildCircuitBreakerThresholds(config.getConfig(DEFAULT_THRESHOLDS)));
    } else {
      builder.defaultThresholds(buildCircuitBreakerDefaultThresholds());
    }

    Map<String, CircuitBreakerThresholds> circuitBreakerThresholdsMap =
        config.root().keySet().stream()
            .filter(key -> !NON_THRESHOLD_KEYS.contains(key)) // Filter out non-threshold keys
            .collect(
                Collectors.toMap(
                    key -> key, // Circuit breaker key
                    key -> buildCircuitBreakerThresholds(config.getConfig(key))));
    builder.circuitBreakerThresholdsMap(circuitBreakerThresholdsMap);
    log.debug("Loaded circuit breaker configs: {}", builder);
    return builder;
  }

  private static CircuitBreakerThresholds buildCircuitBreakerThresholds(Config config) {
    CircuitBreakerThresholds.CircuitBreakerThresholdsBuilder builder =
        CircuitBreakerThresholds.builder();

    if (config.hasPath(FAILURE_RATE_THRESHOLD)) {
      builder.failureRateThreshold((float) config.getDouble(FAILURE_RATE_THRESHOLD));
    }

    if (config.hasPath(SLOW_CALL_RATE_THRESHOLD)) {
      builder.slowCallRateThreshold((float) config.getDouble(SLOW_CALL_RATE_THRESHOLD));
    }

    if (config.hasPath(SLOW_CALL_DURATION_THRESHOLD)) {
      builder.slowCallDurationThreshold(config.getDuration(SLOW_CALL_DURATION_THRESHOLD));
    }

    if (config.hasPath(SLIDING_WINDOW_TYPE)) {
      builder.slidingWindowType(getSlidingWindowType(config.getString(SLIDING_WINDOW_TYPE)));
    }

    if (config.hasPath(SLIDING_WINDOW_SIZE)) {
      builder.slidingWindowSize(config.getInt(SLIDING_WINDOW_SIZE));
    }

    if (config.hasPath(WAIT_DURATION_IN_OPEN_STATE)) {
      builder.waitDurationInOpenState(config.getDuration(WAIT_DURATION_IN_OPEN_STATE));
    }

    if (config.hasPath(PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE)) {
      builder.permittedNumberOfCallsInHalfOpenState(
          config.getInt(PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE));
    }

    if (config.hasPath(MINIMUM_NUMBER_OF_CALLS)) {
      builder.minimumNumberOfCalls(config.getInt(MINIMUM_NUMBER_OF_CALLS));
    }

    return builder.build();
  }

  public static CircuitBreakerThresholds buildCircuitBreakerDefaultThresholds() {
    return CircuitBreakerThresholds.builder().build();
  }

  private static CircuitBreakerThresholds.SlidingWindowType getSlidingWindowType(
      String slidingWindowType) {
    return CircuitBreakerThresholds.SlidingWindowType.valueOf(slidingWindowType);
  }
}
