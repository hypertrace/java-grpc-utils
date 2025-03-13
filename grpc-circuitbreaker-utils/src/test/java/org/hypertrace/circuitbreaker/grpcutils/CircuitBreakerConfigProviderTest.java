package org.hypertrace.circuitbreaker.grpcutils;

import static org.junit.jupiter.api.Assertions.*;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CircuitBreakerConfigProviderTest {

  private CircuitBreakerConfigProvider configProvider;

  @BeforeEach
  public void setUp() {
    Config config =
        ConfigFactory.parseString(
            "enabled=true\n"
                + "default {\n"
                + "  failureRateThreshold=50.0\n"
                + "  slowCallRateThreshold=100.0\n"
                + "  slowCallDurationThreshold=5s\n"
                + "  slidingWindowSize=10\n"
                + "  waitDurationInOpenState=1m\n"
                + "  minimumNumberOfCalls=5\n"
                + "  permittedNumberOfCallsInHalfOpenState=3\n"
                + "  slidingWindowType=COUNT_BASED\n"
                + "}");
    configProvider = new CircuitBreakerConfigProvider(config);
  }

  @Test
  public void testIsCircuitBreakerEnabled() {
    assertTrue(configProvider.isCircuitBreakerEnabled());
  }

  @Test
  public void testGetConfigMap() {
    Map<String, CircuitBreakerConfiguration> configMap = configProvider.getConfigMap();
    assertEquals(1, configMap.size());
    assertTrue(configMap.containsKey("default"));

    CircuitBreakerConfiguration defaultConfig = configMap.get("default");
    assertEquals(50.0f, defaultConfig.getFailureRateThreshold());
    assertEquals(100.0f, defaultConfig.getSlowCallRateThreshold());
    assertEquals(java.time.Duration.ofSeconds(5), defaultConfig.getSlowCallDurationThreshold());
    assertEquals(10, defaultConfig.getSlidingWindowSize());
    assertEquals(java.time.Duration.ofMinutes(1), defaultConfig.getWaitDurationInOpenState());
    assertEquals(5, defaultConfig.getMinimumNumberOfCalls());
    assertEquals(3, defaultConfig.getPermittedNumberOfCallsInHalfOpenState());
    assertEquals(
        CircuitBreakerConfiguration.SlidingWindowType.COUNT_BASED,
        defaultConfig.getSlidingWindowType());
  }
}
