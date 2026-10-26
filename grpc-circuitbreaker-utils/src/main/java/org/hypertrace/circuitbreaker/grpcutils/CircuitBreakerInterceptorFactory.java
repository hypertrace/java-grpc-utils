package org.hypertrace.circuitbreaker.grpcutils;

import com.typesafe.config.Config;
import io.grpc.ClientInterceptor;
import jakarta.inject.Inject;
import java.time.Clock;
import lombok.AllArgsConstructor;
import org.hypertrace.circuitbreaker.grpcutils.resilience.ResilienceCircuitBreakerFactory;

@AllArgsConstructor(onConstructor_ = @Inject)
public class CircuitBreakerInterceptorFactory {
  private final Clock clock;

  public static ClientInterceptor forService(Config rootConfig) {
    return ServiceCircuitBreakerInterceptor.create(rootConfig);
  }

  public CircuitBreakerInterceptor buildInterceptor(CircuitBreakerConfiguration<?> configuration) {
    return ResilienceCircuitBreakerFactory.getCircuitBreakerInterceptor(configuration, clock);
  }
}
