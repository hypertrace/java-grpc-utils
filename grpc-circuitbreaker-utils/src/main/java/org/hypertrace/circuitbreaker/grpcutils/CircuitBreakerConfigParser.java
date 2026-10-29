package org.hypertrace.circuitbreaker.grpcutils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigUtil;
import io.grpc.Status;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CircuitBreakerConfigParser {

  // Percentage of failures to trigger OPEN state
  private static final String FAILURE_RATE_THRESHOLD = "failureRateThreshold";
  // Percentage of slow calls to trigger OPEN state
  private static final String SLOW_CALL_RATE_THRESHOLD = "slowCallRateThreshold";
  // Define what a "slow" call is
  private static final String SLOW_CALL_DURATION_THRESHOLD = "slowCallDurationThreshold";
  // Number of calls to consider in the sliding window
  private static final String SLIDING_WINDOW_SIZE = "slidingWindowSize";
  // Time before retrying after OPEN state
  private static final String WAIT_DURATION_IN_OPEN_STATE = "waitDurationInOpenState";
  // Minimum calls before evaluating failure rate
  private static final String MINIMUM_NUMBER_OF_CALLS = "minimumNumberOfCalls";
  // Calls allowed in HALF_OPEN state before deciding to
  // CLOSE or OPEN again
  private static final String PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE =
      "permittedNumberOfCallsInHalfOpenState";
  private static final String SLIDING_WINDOW_TYPE = "slidingWindowType";
  private static final String FAILURE_STATUS_CODES = "failureStatusCodes";
  static final String CIRCUIT_BREAKER_CONFIG = "circuit.breaker.config";
  public static final String ENABLED = "enabled";
  public static final String DEFAULT_THRESHOLDS = "defaultThresholds";
  private static final Set<String> NON_THRESHOLD_KEYS = Set.of(ENABLED, DEFAULT_THRESHOLDS);

  public static <T> CircuitBreakerConfiguration.CircuitBreakerConfigurationBuilder<T> parseConfig(
      Config config) {
    CircuitBreakerConfiguration.CircuitBreakerConfigurationBuilder<T> builder =
        CircuitBreakerConfiguration.builder();
    if (config.hasPath(ENABLED)) {
      builder.enabled(config.getBoolean(ENABLED));
    }

    Map<String, CircuitBreakerThresholds> circuitBreakerThresholdsMap =
        config.root().keySet().stream()
            .filter(key -> !NON_THRESHOLD_KEYS.contains(key)) // Filter out non-threshold keys
            .collect(
                Collectors.toMap(
                    key -> key, // Circuit breaker key
                    key -> buildCircuitBreakerThresholds(config.getConfig(key))));

    builder.defaultThresholds(
        config.hasPath(DEFAULT_THRESHOLDS)
            ? buildCircuitBreakerThresholds(config.getConfig(DEFAULT_THRESHOLDS))
            : buildCircuitBreakerDefaultThresholds());

    builder.circuitBreakerThresholdsMap(circuitBreakerThresholdsMap);
    log.debug("Loaded circuit breaker configs: {}", builder);
    return builder;
  }

  /**
   * Service-keyed thresholds for {@code circuit.breaker.config}. An absent or disabled block is an
   * empty map. Defaults come from {@link CircuitBreakerThresholds}; config only overlays fields
   * that are present.
   */
  public static Map<String, CircuitBreakerThresholds> parseServiceThresholds(
      final Config rootConfig) {
    if (rootConfig == null || !rootConfig.hasPath(CIRCUIT_BREAKER_CONFIG)) {
      log.debug("circuit.breaker.config is absent; no service circuit breakers installed");
      return Map.of();
    }
    final Config config = rootConfig.getConfig(CIRCUIT_BREAKER_CONFIG);
    if (!config.hasPath(ENABLED) || !config.getBoolean(ENABLED)) {
      log.debug("circuit.breaker.config is disabled; no service circuit breakers installed");
      return Map.of();
    }
    final CircuitBreakerThresholds defaults =
        config.hasPath(DEFAULT_THRESHOLDS)
            ? overlay(buildCircuitBreakerDefaultThresholds(), config.getConfig(DEFAULT_THRESHOLDS))
            : buildCircuitBreakerDefaultThresholds();
    final Map<String, CircuitBreakerThresholds> thresholdsByService = new LinkedHashMap<>();
    for (final String serviceName : config.root().keySet()) {
      if (ENABLED.equals(serviceName) || DEFAULT_THRESHOLDS.equals(serviceName)) {
        continue;
      }
      // getConfig(key) treats dots as path separators, so service names are read with joinPath.
      final CircuitBreakerThresholds thresholds =
          overlay(defaults, config.getConfig(ConfigUtil.joinPath(serviceName)));
      if (!thresholds.isEnabled()) {
        log.debug("Service circuit breaker disabled for {}", serviceName);
        continue;
      }
      thresholdsByService.put(serviceName, thresholds);
    }
    return Map.copyOf(thresholdsByService);
  }

  public static CircuitBreakerThresholds overlay(
      final CircuitBreakerThresholds base, final Config overrides) {
    return applyOverrides(base.toBuilder(), overrides).build();
  }

  private static CircuitBreakerThresholds buildCircuitBreakerThresholds(Config config) {
    return overlay(buildCircuitBreakerDefaultThresholds(), config);
  }

  private static CircuitBreakerThresholds.CircuitBreakerThresholdsBuilder applyOverrides(
      final CircuitBreakerThresholds.CircuitBreakerThresholdsBuilder builder, final Config config) {

    if (config.hasPath(FAILURE_RATE_THRESHOLD)) {
      builder.failureRateThreshold((float) config.getDouble(FAILURE_RATE_THRESHOLD));
    }

    if (config.hasPath(SLOW_CALL_RATE_THRESHOLD)) {
      builder.slowCallRateThreshold((float) config.getDouble(SLOW_CALL_RATE_THRESHOLD));
    }

    if (config.hasPath(SLOW_CALL_DURATION_THRESHOLD)) {
      builder.slowCallDurationThreshold(config.getDuration(SLOW_CALL_DURATION_THRESHOLD));
    }

    if (config.hasPath(SLIDING_WINDOW_TYPE)) {
      builder.slidingWindowType(getSlidingWindowType(config.getString(SLIDING_WINDOW_TYPE)));
    }

    if (config.hasPath(SLIDING_WINDOW_SIZE)) {
      builder.slidingWindowSize(config.getInt(SLIDING_WINDOW_SIZE));
    }

    if (config.hasPath(WAIT_DURATION_IN_OPEN_STATE)) {
      builder.waitDurationInOpenState(config.getDuration(WAIT_DURATION_IN_OPEN_STATE));
    }

    if (config.hasPath(PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE)) {
      builder.permittedNumberOfCallsInHalfOpenState(
          config.getInt(PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE));
    }

    if (config.hasPath(MINIMUM_NUMBER_OF_CALLS)) {
      builder.minimumNumberOfCalls(config.getInt(MINIMUM_NUMBER_OF_CALLS));
    }

    if (config.hasPath(ENABLED)) {
      builder.enabled(config.getBoolean(ENABLED));
    }

    if (config.hasPath(FAILURE_STATUS_CODES)) {
      builder.failureStatusCodes(
          parseFailureStatusCodes(config.getStringList(FAILURE_STATUS_CODES)));
    }

    return builder;
  }

  // An unknown name must fail startup. Ignoring it would protect the wrong calls.
  private static Set<Status.Code> parseFailureStatusCodes(List<String> names) {
    Set<Status.Code> codes = EnumSet.noneOf(Status.Code.class);
    for (String name : names) {
      try {
        codes.add(Status.Code.valueOf(name));
      } catch (IllegalArgumentException exception) {
        throw new ConfigException.BadValue(FAILURE_STATUS_CODES, name);
      }
    }
    return Set.copyOf(codes);
  }

  public static CircuitBreakerThresholds buildCircuitBreakerDefaultThresholds() {
    return CircuitBreakerThresholds.builder().build();
  }

  private static CircuitBreakerThresholds.SlidingWindowType getSlidingWindowType(
      String slidingWindowType) {
    return CircuitBreakerThresholds.SlidingWindowType.valueOf(slidingWindowType);
  }
}
