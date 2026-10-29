package org.hypertrace.circuitbreaker.grpcutils;

import com.typesafe.config.Config;
import io.grpc.ClientInterceptor;
import jakarta.inject.Inject;
import java.time.Clock;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.hypertrace.circuitbreaker.grpcutils.resilience.ResilienceCircuitBreakerFactory;

@AllArgsConstructor(onConstructor_ = @Inject)
public class CircuitBreakerInterceptorFactory {
  private final Clock clock;

  public static ClientInterceptor forService(final Config rootConfig) {
    return forService(CircuitBreakerConfigParser.parseServiceThresholds(rootConfig));
  }

  public static ClientInterceptor forService(
      final Map<String, CircuitBreakerThresholds> thresholdsByService) {
    return ServiceCircuitBreakerInterceptor.create(thresholdsByService);
  }

  public CircuitBreakerInterceptor buildInterceptor(CircuitBreakerConfiguration<?> configuration) {
    return ResilienceCircuitBreakerFactory.getCircuitBreakerInterceptor(configuration, clock);
  }
}
