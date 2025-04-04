package org.hypertrace.circuitbreaker.grpcutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.util.Map;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerThresholds.SlidingWindowType;
import org.junit.jupiter.api.Test;

class CircuitBreakerConfigParserTest {

  @Test
  void testParseConfig() {
    String configStr =
        "enabled = true\n" +
            "defaultThresholds {\n" +
            "  failureRateThreshold = 50.0\n" +
            "  slowCallRateThreshold = 30.0\n" +
            "  slowCallDurationThreshold = 1s\n" +
            "  slidingWindowSize = 100\n" +
            "  slidingWindowType = COUNT_BASED\n" +
            "}\n" +
            "service1 {\n" +
            "  failureRateThreshold = 60.0\n" +
            "  slowCallRateThreshold = 40.0\n" +
            "  slowCallDurationThreshold = 2s\n" +
            "  slidingWindowSize = 200\n" +
            "  slidingWindowType = TIME_BASED\n" +
            "}";

    Config config = ConfigFactory.parseString(configStr);
    CircuitBreakerConfiguration.CircuitBreakerConfigurationBuilder<Object> builder =
        CircuitBreakerConfigParser.parseConfig(config);
    CircuitBreakerConfiguration<Object> configuration = builder.build();

    // Test enabled flag
    assertTrue(configuration.isEnabled());

    // Test default thresholds
    CircuitBreakerThresholds defaultThresholds = configuration.getDefaultThresholds();
    assertNotNull(defaultThresholds);
    assertEquals(50.0f, defaultThresholds.getFailureRateThreshold());
    assertEquals(30.0f, defaultThresholds.getSlowCallRateThreshold());
    assertEquals(Duration.ofSeconds(1), defaultThresholds.getSlowCallDurationThreshold());
    assertEquals(100, defaultThresholds.getSlidingWindowSize());
    assertEquals(SlidingWindowType.COUNT_BASED, defaultThresholds.getSlidingWindowType());

    // Test service specific thresholds
    Map<String, CircuitBreakerThresholds> thresholdsMap = configuration.getCircuitBreakerThresholdsMap();
    assertNotNull(thresholdsMap);
    assertTrue(thresholdsMap.containsKey("service1"));

    CircuitBreakerThresholds service1Thresholds = thresholdsMap.get("service1");
    assertEquals(60.0f, service1Thresholds.getFailureRateThreshold());
    assertEquals(40.0f, service1Thresholds.getSlowCallRateThreshold());
    assertEquals(Duration.ofSeconds(2), service1Thresholds.getSlowCallDurationThreshold());
    assertEquals(200, service1Thresholds.getSlidingWindowSize());
    assertEquals(SlidingWindowType.TIME_BASED, service1Thresholds.getSlidingWindowType());
  }

  @Test
  void testParseConfigWithMinimalConfig() {
    String configStr = "{}";
    Config config = ConfigFactory.parseString(configStr);
    CircuitBreakerConfiguration.CircuitBreakerConfigurationBuilder<Object> builder =
        CircuitBreakerConfigParser.parseConfig(config);
    CircuitBreakerConfiguration<Object> configuration = builder.build();

    // Test that defaults are used when no config is provided
    assertFalse(configuration.isEnabled());
    assertNotNull(configuration.getDefaultThresholds());
    assertTrue(configuration.getCircuitBreakerThresholdsMap().isEmpty());
  }
}
