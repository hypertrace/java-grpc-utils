package org.hypertrace.circuitbreaker.grpcutils;

import jakarta.inject.Inject;
import java.time.Clock;
import lombok.AllArgsConstructor;
import org.hypertrace.circuitbreaker.grpcutils.resilience.ResilienceCircuitBreakerFactory;

@AllArgsConstructor(onConstructor_ = @Inject)
public class CircuitBreakerInterceptorFactory {
  private final Clock clock;

  public CircuitBreakerInterceptor buildInterceptor(CircuitBreakerConfiguration<?> configuration) {
    return ResilienceCircuitBreakerFactory.getCircuitBreakerInterceptor(configuration, clock);
  }
}
