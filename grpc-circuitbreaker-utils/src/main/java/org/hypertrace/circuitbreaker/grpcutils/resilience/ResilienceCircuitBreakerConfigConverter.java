package org.hypertrace.circuitbreaker.grpcutils.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.util.Map;
import java.util.stream.Collectors;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerThresholds;

/** Utility class to parse CircuitBreakerConfiguration to Resilience4j CircuitBreakerConfig */
public class ResilienceCircuitBreakerConfigConverter {

  public static Map<String, CircuitBreakerConfig> getCircuitBreakerConfigs(
      Map<String, CircuitBreakerThresholds> configurationMap) {
    return configurationMap.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> convertConfig(entry.getValue())));
  }

  static CircuitBreakerConfig convertConfig(CircuitBreakerThresholds configuration) {
    return CircuitBreakerConfig.custom()
        .failureRateThreshold(configuration.getFailureRateThreshold())
        .slowCallRateThreshold(configuration.getSlowCallRateThreshold())
        .slowCallDurationThreshold(configuration.getSlowCallDurationThreshold())
        .slidingWindowType(getSlidingWindowType(configuration.getSlidingWindowType()))
        .slidingWindowSize(configuration.getSlidingWindowSize())
        .waitDurationInOpenState(configuration.getWaitDurationInOpenState())
        .permittedNumberOfCallsInHalfOpenState(
            configuration.getPermittedNumberOfCallsInHalfOpenState())
        .minimumNumberOfCalls(configuration.getMinimumNumberOfCalls())
        .build();
  }

  private static CircuitBreakerConfig.SlidingWindowType getSlidingWindowType(
      CircuitBreakerThresholds.SlidingWindowType slidingWindowType) {
    switch (slidingWindowType) {
      case COUNT_BASED:
        return CircuitBreakerConfig.SlidingWindowType.COUNT_BASED;
      case TIME_BASED:
      default:
        return CircuitBreakerConfig.SlidingWindowType.TIME_BASED;
    }
  }
}
