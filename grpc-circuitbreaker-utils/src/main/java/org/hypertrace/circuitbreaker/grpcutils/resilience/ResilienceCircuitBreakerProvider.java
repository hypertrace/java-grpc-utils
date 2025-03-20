package org.hypertrace.circuitbreaker.grpcutils.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerConfigParser;

/** Utility class to provide Resilience4j CircuitBreaker */
@Slf4j
class ResilienceCircuitBreakerProvider {

  private static final String SHARED_KEY = "SHARED_KEY";
  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final Map<String, CircuitBreakerConfig> circuitBreakerConfigMap;
  private final Map<String, CircuitBreaker> circuitBreakerCache = new ConcurrentHashMap<>();
  private final List<String> disabledKeys;
  private final boolean defaultEnabled;

  public ResilienceCircuitBreakerProvider(
      CircuitBreakerRegistry circuitBreakerRegistry,
      Map<String, CircuitBreakerConfig> circuitBreakerConfigMap,
      List<String> disabledKeys) {
    this.circuitBreakerRegistry = circuitBreakerRegistry;
    this.circuitBreakerConfigMap = circuitBreakerConfigMap;
    this.disabledKeys = disabledKeys;
    this.defaultEnabled = !disabledKeys.contains(CircuitBreakerConfigParser.DEFAULT_THRESHOLDS);
  }

  public Optional<CircuitBreaker> getCircuitBreaker(String circuitBreakerKey) {
    if (disabledKeys.contains(circuitBreakerKey)) {
      return Optional.empty();
    }
    return Optional.ofNullable(
        circuitBreakerCache.computeIfAbsent(
            circuitBreakerKey,
            key -> {
              CircuitBreaker circuitBreaker =
                  getCircuitBreakerFromConfigMap(circuitBreakerKey, defaultEnabled);
              // If no circuit breaker is created return empty
              if (circuitBreaker == null) {
                return null; // Ensures cache does not store null entries
              }
              attachListeners(circuitBreaker);
              return circuitBreaker;
            }));
  }

  public Optional<CircuitBreaker> getSharedCircuitBreaker() {
    if (!defaultEnabled) {
      return Optional.empty();
    }
    return Optional.of(
        circuitBreakerCache.computeIfAbsent(
            SHARED_KEY,
            key -> {
              CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(SHARED_KEY);
              attachListeners(circuitBreaker);
              return circuitBreaker;
            }));
  }

  private static void attachListeners(CircuitBreaker circuitBreaker) {
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
  }

  private CircuitBreaker getCircuitBreakerFromConfigMap(
      String circuitBreakerKey, boolean defaultEnabled) {
    return Optional.ofNullable(circuitBreakerConfigMap.get(circuitBreakerKey))
        .map(config -> circuitBreakerRegistry.circuitBreaker(circuitBreakerKey, config))
        .orElseGet(
            () ->
                defaultEnabled
                    ? circuitBreakerRegistry.circuitBreaker(circuitBreakerKey)
                    : null); // Return null if default is disabled
  }
}
