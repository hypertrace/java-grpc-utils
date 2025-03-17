package org.hypertrace.circuitbreaker.grpcutils.resilience;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerThresholds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResilienceCircuitBreakerConfigConverterTest {

  @Test
  void shouldParseValidConfiguration() {
    CircuitBreakerThresholds thresholds =
        CircuitBreakerThresholds.builder()
            .failureRateThreshold(50.0f)
            .slowCallRateThreshold(30.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            .slidingWindowType(CircuitBreakerThresholds.SlidingWindowType.TIME_BASED)
            .slidingWindowSize(100)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .permittedNumberOfCallsInHalfOpenState(5)
            .minimumNumberOfCalls(20)
            .build();

    Map<String, CircuitBreakerThresholds> configMap = new HashMap<>();
    configMap.put("testService", thresholds);

    Map<String, CircuitBreakerConfig> result =
        ResilienceCircuitBreakerConfigConverter.getCircuitBreakerConfigs(configMap);

    Assertions.assertTrue(result.containsKey("testService"));

    CircuitBreakerConfig config = result.get("testService");
    assertEquals(50.0f, config.getFailureRateThreshold());
    assertEquals(30.0f, config.getSlowCallRateThreshold());
    assertEquals(Duration.ofSeconds(2), config.getSlowCallDurationThreshold());
    assertEquals(CircuitBreakerConfig.SlidingWindowType.TIME_BASED, config.getSlidingWindowType());
    assertEquals(100, config.getSlidingWindowSize());
    assertEquals(5, config.getPermittedNumberOfCallsInHalfOpenState());
    assertEquals(20, config.getMinimumNumberOfCalls());
  }

  @Test
  void shouldThrowExceptionWhenConfigurationIsNull() {
    assertThrows(
        NullPointerException.class,
        () -> ResilienceCircuitBreakerConfigConverter.convertConfig(null));
  }
}
