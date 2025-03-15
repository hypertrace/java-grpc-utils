package org.hypertrace.circuitbreaker.grpcutils.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/** Utility class to provide Resilience4j CircuitBreaker */
@Slf4j
@Singleton
public class ResilienceCircuitBreakerProvider {

  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final Map<String, CircuitBreakerConfig> circuitBreakerConfigMap;
  private final Map<String, CircuitBreaker> circuitBreakerCache = new ConcurrentHashMap<>();

  public ResilienceCircuitBreakerProvider(
      CircuitBreakerRegistry circuitBreakerRegistry,
      Map<String, CircuitBreakerConfig> circuitBreakerConfigMap) {
    this.circuitBreakerRegistry = circuitBreakerRegistry;
    this.circuitBreakerConfigMap = circuitBreakerConfigMap;
  }

  public CircuitBreaker getCircuitBreaker(String circuitBreakerKey) {
    return circuitBreakerCache.computeIfAbsent(
        circuitBreakerKey,
        key -> {
          CircuitBreaker circuitBreaker =
              circuitBreakerRegistry.circuitBreaker(
                  circuitBreakerKey,
                  circuitBreakerConfigMap.getOrDefault(
                      circuitBreakerKey, circuitBreakerConfigMap.get("default")));
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
        });
  }
}
