package org.hypertrace.circuitbreaker.grpcutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import io.grpc.Status.Code;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CircuitBreakerConfigParserTest {

  @Test
  void omittedFailureStatusesUseTheLibraryDefault() {
    CircuitBreakerThresholds thresholds =
        CircuitBreakerConfigParser.parseConfig(ConfigFactory.parseString("enabled = true"))
            .build()
            .getDefaultThresholds();

    assertEquals(
        Set.of(Code.DEADLINE_EXCEEDED, Code.UNAVAILABLE, Code.RESOURCE_EXHAUSTED),
        thresholds.getFailureStatusCodes());
    assertEquals(Duration.ofSeconds(2), thresholds.getSlowCallDurationThreshold());
  }

  @Test
  void defaultThresholdsReplaceFailureStatuses() {
    CircuitBreakerThresholds thresholds =
        CircuitBreakerConfigParser.parseConfig(
                ConfigFactory.parseString(
                    "defaultThresholds { failureStatusCodes = [UNAVAILABLE] }"))
            .build()
            .getDefaultThresholds();

    assertEquals(Set.of(Code.UNAVAILABLE), thresholds.getFailureStatusCodes());
  }

  @Test
  void unknownFailureStatusFailsParsing() {
    assertThrows(
        ConfigException.BadValue.class,
        () ->
            CircuitBreakerConfigParser.parseConfig(
                ConfigFactory.parseString(
                    "defaultThresholds { failureStatusCodes = [NOT_A_STATUS] }")));
  }
}
