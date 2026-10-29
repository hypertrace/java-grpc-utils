package org.hypertrace.circuitbreaker.grpcutils;

import com.typesafe.config.Config;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.core.grpcutils.client.GrpcChannelRegistry;
import org.hypertrace.core.grpcutils.client.InterceptingChannelRegistry;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CircuitBreakerChannelRegistry {

  static final String CIRCUIT_BREAKER_CONFIG = CircuitBreakerConfigParser.CIRCUIT_BREAKER_CONFIG;

  public static GrpcChannelRegistry wrap(final GrpcChannelRegistry registry, final Config config) {
    if (registry == null || config == null || !config.hasPath(CIRCUIT_BREAKER_CONFIG)) {
      log.debug("circuit.breaker.config is absent; leaving the channel registry unchanged");
      return registry;
    }
    final Config circuitBreakerConfig = config.getConfig(CIRCUIT_BREAKER_CONFIG);
    if (!circuitBreakerConfig.hasPath("enabled") || !circuitBreakerConfig.getBoolean("enabled")) {
      log.debug("circuit.breaker.config is disabled; leaving the channel registry unchanged");
      return registry;
    }
    return InterceptingChannelRegistry.wrap(
        registry, CircuitBreakerInterceptorFactory.forService(config));
  }
}
