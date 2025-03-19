package org.hypertrace.circuitbreaker.grpcutils.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Clock;
import java.util.Map;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerConfiguration;

public class ResilienceCircuitBreakerFactory {
  public static ResilienceCircuitBreakerInterceptor getResilienceCircuitBreakerInterceptor(
      CircuitBreakerConfiguration<?> circuitBreakerConfiguration, Clock clock) {
    Map<String, CircuitBreakerConfig> resilienceCircuitBreakerConfigMap =
        ResilienceCircuitBreakerConfigConverter.getCircuitBreakerConfigs(
            circuitBreakerConfiguration.getCircuitBreakerThresholdsMap());
    CircuitBreakerRegistry resilicenceCircuitBreakerRegistry =
        new ResilienceCircuitBreakerRegistryProvider(
                circuitBreakerConfiguration.getDefaultThresholds())
            .getCircuitBreakerRegistry();
    ResilienceCircuitBreakerProvider resilienceCircuitBreakerProvider =
        new ResilienceCircuitBreakerProvider(
            resilicenceCircuitBreakerRegistry,
            resilienceCircuitBreakerConfigMap,
            circuitBreakerConfiguration.getCircuitBreakerThresholdsMap());
    return new ResilienceCircuitBreakerInterceptor(
        circuitBreakerConfiguration, clock, resilienceCircuitBreakerProvider);
  }
}
