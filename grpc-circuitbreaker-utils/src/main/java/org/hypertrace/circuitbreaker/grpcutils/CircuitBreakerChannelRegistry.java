package org.hypertrace.circuitbreaker.grpcutils;

import com.typesafe.config.Config;
import java.util.Map;
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
    return wrap(registry, CircuitBreakerConfigParser.parseServiceThresholds(config));
  }

  public static GrpcChannelRegistry wrap(
      final GrpcChannelRegistry registry,
      final Map<String, CircuitBreakerThresholds> thresholdsByService) {
    if (registry == null || thresholdsByService == null || thresholdsByService.isEmpty()) {
      log.debug("No enabled service circuit breakers; leaving the channel registry unchanged");
      return registry;
    }
    return InterceptingChannelRegistry.wrap(
        registry, CircuitBreakerInterceptorFactory.forService(thresholdsByService));
  }
}
