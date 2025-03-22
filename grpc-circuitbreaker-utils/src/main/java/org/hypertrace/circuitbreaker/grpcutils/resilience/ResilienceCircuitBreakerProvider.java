package org.hypertrace.circuitbreaker.grpcutils.resilience;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/** Utility class to provide Resilience4j CircuitBreaker */
@Slf4j
class ResilienceCircuitBreakerProvider {

  private static final String SHARED_KEY = "SHARED_KEY";
  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final Map<String, CircuitBreakerConfig> circuitBreakerConfigMap;
  private final List<String> disabledKeys;
  private final boolean defaultEnabled;

  // LoadingCache to manage CircuitBreaker instances with automatic loading and eviction
  private final LoadingCache<String, Optional<CircuitBreaker>> circuitBreakerCache =
      CacheBuilder.newBuilder()
          .expireAfterAccess(60, TimeUnit.MINUTES) // Auto-evict after 60 minutes
          .maximumSize(10000) // Limit max cache size
          .build(CacheLoader.from(this::buildNewCircuitBreaker));

  public ResilienceCircuitBreakerProvider(
      CircuitBreakerRegistry circuitBreakerRegistry,
      Map<String, CircuitBreakerConfig> circuitBreakerConfigMap,
      List<String> disabledKeys,
      boolean defaultEnabled) {
    this.circuitBreakerRegistry = circuitBreakerRegistry;
    this.circuitBreakerConfigMap = circuitBreakerConfigMap;
    this.disabledKeys = disabledKeys;
    this.defaultEnabled = defaultEnabled;
  }

  public Optional<CircuitBreaker> getCircuitBreaker(String circuitBreakerKey) {
    if (disabledKeys.contains(circuitBreakerKey)) {
      return Optional.empty();
    }
    return circuitBreakerCache.getUnchecked(circuitBreakerKey);
  }

  public Optional<CircuitBreaker> getSharedCircuitBreaker() {
    return defaultEnabled ? getCircuitBreaker(SHARED_KEY) : Optional.empty();
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

  private Optional<CircuitBreaker> buildNewCircuitBreaker(String circuitBreakerKey) {
    return Optional.ofNullable(circuitBreakerConfigMap.get(circuitBreakerKey))
        .map(config -> circuitBreakerRegistry.circuitBreaker(circuitBreakerKey, config))
        .or(
            () ->
                defaultEnabled
                    ? Optional.of(circuitBreakerRegistry.circuitBreaker(circuitBreakerKey))
                    : Optional.empty())
        .map(
            circuitBreaker -> {
              attachListeners(circuitBreaker);
              return circuitBreaker;
            });
  }
}
