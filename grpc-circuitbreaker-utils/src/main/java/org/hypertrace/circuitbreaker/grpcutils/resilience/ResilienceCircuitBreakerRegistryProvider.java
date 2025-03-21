package org.hypertrace.circuitbreaker.grpcutils.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerThresholds;

/** Utility class to provide Resilience4j CircuitBreakerRegistry */
@Slf4j
class ResilienceCircuitBreakerRegistryProvider {
  private final CircuitBreakerThresholds circuitBreakerThresholds;

  public ResilienceCircuitBreakerRegistryProvider(
      CircuitBreakerThresholds circuitBreakerThresholds) {
    this.circuitBreakerThresholds = circuitBreakerThresholds;
  }

  public CircuitBreakerRegistry getCircuitBreakerRegistry() {
    CircuitBreakerRegistry circuitBreakerRegistry =
        CircuitBreakerRegistry.of(
            ResilienceCircuitBreakerConfigConverter.convertConfig(circuitBreakerThresholds));
    circuitBreakerRegistry
        .getEventPublisher()
        .onEntryAdded(
            entryAddedEvent -> {
              CircuitBreaker addedCircuitBreaker = entryAddedEvent.getAddedEntry();
              log.debug("CircuitBreaker {} added", addedCircuitBreaker.getName());
            })
        .onEntryRemoved(
            entryRemovedEvent -> {
              CircuitBreaker removedCircuitBreaker = entryRemovedEvent.getRemovedEntry();
              log.debug("CircuitBreaker {} removed", removedCircuitBreaker.getName());
            });
    return circuitBreakerRegistry;
  }
}
