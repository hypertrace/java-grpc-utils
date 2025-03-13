package org.hypertrace.circuitbreaker.grpcutils.resilience;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerConfiguration;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerConfiguration.SlidingWindowType;
import org.junit.jupiter.api.Test;

public class ResilienceCircuitBreakerConfigParserTest {

  @Test
  void testGetConfigWithCountBasedSlidingWindow() {
    CircuitBreakerConfiguration configuration =
        CircuitBreakerConfiguration.builder()
            .failureRateThreshold(50.0f)
            .slowCallRateThreshold(30.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            .slidingWindowType(SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(100)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(10)
            .minimumNumberOfCalls(20)
            .build();

    CircuitBreakerConfig config = ResilienceCircuitBreakerConfigParser.getConfig(configuration);

    assertEquals(50.0f, config.getFailureRateThreshold());
    assertEquals(30.0f, config.getSlowCallRateThreshold());
    assertEquals(Duration.ofSeconds(2), config.getSlowCallDurationThreshold());
    assertEquals(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED, config.getSlidingWindowType());
    assertEquals(100, config.getSlidingWindowSize());
    assertEquals(Duration.ofSeconds(30), config.getWaitDurationInOpenState());
    assertEquals(10, config.getPermittedNumberOfCallsInHalfOpenState());
    assertEquals(20, config.getMinimumNumberOfCalls());
  }

  @Test
  void testGetConfigWithTimeBasedSlidingWindow() {
    CircuitBreakerConfiguration configuration =
        CircuitBreakerConfiguration.builder()
            .failureRateThreshold(70.0f)
            .slowCallRateThreshold(40.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(5))
            .slidingWindowType(SlidingWindowType.TIME_BASED)
            .slidingWindowSize(60)
            .waitDurationInOpenState(Duration.ofMinutes(1))
            .permittedNumberOfCallsInHalfOpenState(5)
            .minimumNumberOfCalls(15)
            .build();

    CircuitBreakerConfig config = ResilienceCircuitBreakerConfigParser.getConfig(configuration);

    assertEquals(70.0f, config.getFailureRateThreshold());
    assertEquals(40.0f, config.getSlowCallRateThreshold());
    assertEquals(Duration.ofSeconds(5), config.getSlowCallDurationThreshold());
    assertEquals(CircuitBreakerConfig.SlidingWindowType.TIME_BASED, config.getSlidingWindowType());
    assertEquals(60, config.getSlidingWindowSize());
    assertEquals(Duration.ofMinutes(1), config.getWaitDurationInOpenState());
    assertEquals(5, config.getPermittedNumberOfCallsInHalfOpenState());
    assertEquals(15, config.getMinimumNumberOfCalls());
  }
}
