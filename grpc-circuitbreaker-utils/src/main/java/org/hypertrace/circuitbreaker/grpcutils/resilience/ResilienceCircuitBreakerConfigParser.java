package org.hypertrace.circuitbreaker.grpcutils.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.util.Map;
import java.util.stream.Collectors;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerConfiguration;

/** Utility class to parse CircuitBreakerConfiguration to Resilience4j CircuitBreakerConfig */
public class ResilienceCircuitBreakerConfigParser {

  public static Map<String, CircuitBreakerConfig> getCircuitBreakerConfigs(
      Map<String, CircuitBreakerConfiguration> configurationMap) {
    return configurationMap.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> getConfig(entry.getValue())));
  }

  static CircuitBreakerConfig getConfig(CircuitBreakerConfiguration configuration) {
    return CircuitBreakerConfig.custom()
        .failureRateThreshold(configuration.getFailureRateThreshold())
        .slowCallRateThreshold(configuration.getSlowCallRateThreshold())
        .slowCallDurationThreshold(configuration.getSlowCallDurationThreshold())
        .slidingWindowType(getSlidingWindowType(configuration))
        .slidingWindowSize(configuration.getSlidingWindowSize())
        .waitDurationInOpenState(configuration.getWaitDurationInOpenState())
        .permittedNumberOfCallsInHalfOpenState(
            configuration.getPermittedNumberOfCallsInHalfOpenState())
        .minimumNumberOfCalls(configuration.getMinimumNumberOfCalls())
        .build();
  }

  private static CircuitBreakerConfig.SlidingWindowType getSlidingWindowType(
      CircuitBreakerConfiguration configuration) {
    switch (configuration.getSlidingWindowType()) {
      case COUNT_BASED:
        return CircuitBreakerConfig.SlidingWindowType.COUNT_BASED;
      case TIME_BASED:
      default:
        return CircuitBreakerConfig.SlidingWindowType.TIME_BASED;
    }
  }
}
