package org.hypertrace.circuitbreaker.grpcutils;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CircuitBreakerEventListener {
  private static final Set<String> attachedCircuitBreakers = ConcurrentHashMap.newKeySet();

  public static synchronized void attachListeners(CircuitBreaker circuitBreaker) {
    if (!attachedCircuitBreakers.add(
        circuitBreaker.getName())) { // Ensures only one listener is attached
      return;
    }
    circuitBreaker
        .getEventPublisher()
        .onStateTransition(
            event ->
                log.info(
                    "State transition: {}  for circuit breaker  {} ",
                    event.getStateTransition(),
                    event.getCircuitBreakerName()))
        .onCallNotPermitted(
            event ->
                log.debug(
                    "Call not permitted: Circuit is OPEN for circuit breaker {} ",
                    event.getCircuitBreakerName()))
        .onEvent(
            event -> {
              log.debug(
                  "Circuit breaker event type {}  for circuit breaker name {} ",
                  event.getEventType(),
                  event.getCircuitBreakerName());
            });
  }
}
