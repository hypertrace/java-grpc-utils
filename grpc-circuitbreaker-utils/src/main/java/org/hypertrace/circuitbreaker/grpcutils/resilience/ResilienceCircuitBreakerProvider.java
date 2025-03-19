package org.hypertrace.circuitbreaker.grpcutils.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerConfigParser;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerThresholds;

/** Utility class to provide Resilience4j CircuitBreaker */
@Slf4j
class ResilienceCircuitBreakerProvider {

  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final Map<String, CircuitBreakerConfig> circuitBreakerConfigMap;
  private final Map<String, CircuitBreakerThresholds> circuitBreakerThresholdsMap;
  private final Map<String, CircuitBreaker> circuitBreakerCache = new ConcurrentHashMap<>();

  public ResilienceCircuitBreakerProvider(
      CircuitBreakerRegistry circuitBreakerRegistry,
      Map<String, CircuitBreakerConfig> circuitBreakerConfigMap,
      Map<String, CircuitBreakerThresholds> circuitBreakerThresholdsMap) {
    this.circuitBreakerRegistry = circuitBreakerRegistry;
    this.circuitBreakerConfigMap = circuitBreakerConfigMap;
    this.circuitBreakerThresholdsMap = circuitBreakerThresholdsMap;
  }

  public Optional<CircuitBreaker> getCircuitBreaker(String circuitBreakerKey) {
    if (circuitBreakerThresholdsMap.containsKey(circuitBreakerKey)
        && !circuitBreakerThresholdsMap.get(circuitBreakerKey).isEnabled()) {
      return Optional.empty();
    }
    return Optional.of(
        circuitBreakerCache.computeIfAbsent(
            circuitBreakerKey,
            key -> {
              CircuitBreaker circuitBreaker = getCircuitBreakerFromConfigMap(circuitBreakerKey);
              circuitBreaker
                  .getEventPublisher()
                  .onStateTransition(
                      event ->
                          log.info(
                              "State transition: {} for circuit breaker {}",
                              event.getStateTransition(),
                              event.getCircuitBreakerName()))
                  .onCallNotPermitted(
                      event ->
                          log.debug(
                              "Call not permitted: Circuit is OPEN for circuit breaker {}",
                              event.getCircuitBreakerName()))
                  .onEvent(
                      event ->
                          log.debug(
                              "Circuit breaker event type {} for circuit breaker name {}",
                              event.getEventType(),
                              event.getCircuitBreakerName()));
              return circuitBreaker;
            }));
  }

  public Optional<CircuitBreaker> getDefaultCircuitBreaker() {
    return getCircuitBreaker(CircuitBreakerConfigParser.DEFAULT_THRESHOLDS);
  }

  private CircuitBreaker getCircuitBreakerFromConfigMap(String circuitBreakerKey) {
    return Optional.ofNullable(circuitBreakerConfigMap.get(circuitBreakerKey))
        .map(config -> circuitBreakerRegistry.circuitBreaker(circuitBreakerKey, config))
        .orElseGet(() -> circuitBreakerRegistry.circuitBreaker(circuitBreakerKey));
  }
}
