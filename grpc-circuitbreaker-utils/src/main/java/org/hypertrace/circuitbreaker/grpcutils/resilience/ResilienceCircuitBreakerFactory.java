package org.hypertrace.circuitbreaker.grpcutils.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Clock;
import java.util.Map;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerConfiguration;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerInterceptor;

public class ResilienceCircuitBreakerFactory {
  public static CircuitBreakerInterceptor getCircuitBreakerInterceptor(
      CircuitBreakerConfiguration<?> circuitBreakerConfiguration, Clock clock) {
    Map<String, CircuitBreakerConfig> resilienceCircuitBreakerConfigMap =
        ResilienceCircuitBreakerConfigConverter.getCircuitBreakerConfigs(
            circuitBreakerConfiguration.getCircuitBreakerThresholdsMap());
    CircuitBreakerRegistry resilienceCircuitBreakerRegistry =
        new ResilienceCircuitBreakerRegistryProvider(
                circuitBreakerConfiguration.getDefaultThresholds())
            .getCircuitBreakerRegistry();
    ResilienceCircuitBreakerProvider resilienceCircuitBreakerProvider =
        new ResilienceCircuitBreakerProvider(
            resilienceCircuitBreakerRegistry,
            resilienceCircuitBreakerConfigMap,
            ResilienceCircuitBreakerConfigConverter.getDisabledKeys(
                circuitBreakerConfiguration.getCircuitBreakerThresholdsMap()),
            circuitBreakerConfiguration.getDefaultThresholds().isEnabled());
    return new ResilienceCircuitBreakerInterceptor(
        circuitBreakerConfiguration, clock, resilienceCircuitBreakerProvider);
  }
}
