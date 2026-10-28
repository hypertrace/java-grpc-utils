package org.hypertrace.circuitbreaker.grpcutils;

import com.typesafe.config.Config;
import org.hypertrace.core.grpcutils.client.GrpcChannelRegistry;
import org.hypertrace.core.grpcutils.client.InterceptingChannelRegistry;

public final class CircuitBreakerChannelRegistry {

  static final String CIRCUIT_BREAKER_CONFIG = "circuit.breaker.config";

  private CircuitBreakerChannelRegistry() {}

  public static GrpcChannelRegistry wrap(GrpcChannelRegistry registry, Config config) {
    if (registry == null || config == null || !config.hasPath(CIRCUIT_BREAKER_CONFIG)) {
      return registry;
    }
    Config circuitBreakerConfig = config.getConfig(CIRCUIT_BREAKER_CONFIG);
    if (!circuitBreakerConfig.hasPath("enabled") || !circuitBreakerConfig.getBoolean("enabled")) {
      return registry;
    }
    return InterceptingChannelRegistry.wrap(
        registry, CircuitBreakerInterceptorFactory.forService(config));
  }
}
