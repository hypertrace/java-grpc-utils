package org.hypertrace.circuitbreaker.grpcutils.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerConfigProvider;

/** Utility class to provide Resilience4j CircuitBreakerRegistry */
@Slf4j
public class ResilienceCircuitBreakerRegistryProvider {
  private final Map<String, CircuitBreakerConfig> circuitBreakerConfigMap;

  public ResilienceCircuitBreakerRegistryProvider(
      Map<String, CircuitBreakerConfig> circuitBreakerConfigMap) {
    this.circuitBreakerConfigMap = circuitBreakerConfigMap;
  }

  public CircuitBreakerRegistry getCircuitBreakerRegistry() {
    CircuitBreakerRegistry circuitBreakerRegistry =
        CircuitBreakerRegistry.of(
            this.circuitBreakerConfigMap.get(CircuitBreakerConfigProvider.DEFAULT_CONFIG_KEY));

    circuitBreakerRegistry
        .getEventPublisher()
        .onEntryAdded(
            entryAddedEvent -> {
              CircuitBreaker addedCircuitBreaker = entryAddedEvent.getAddedEntry();
              log.info("CircuitBreaker {} added", addedCircuitBreaker.getName());
            })
        .onEntryRemoved(
            entryRemovedEvent -> {
              CircuitBreaker removedCircuitBreaker = entryRemovedEvent.getRemovedEntry();
              log.info("CircuitBreaker {} removed", removedCircuitBreaker.getName());
            });
    return circuitBreakerRegistry;
  }
}
