package org.hypertrace.circuitbreaker.grpcutils;

import com.typesafe.config.Config;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CircuitBreakerConfigProvider {

  public static final String CIRCUIT_BREAKER_CONFIG = "circuit.breaker.config";
  public static final String DEFAULT_CONFIG_KEY = "default";

  // Whether to enable circuit breaker or not.
  private static final String ENABLED = "enabled";

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

  // Cache for storing CircuitBreakerConfig instances
  private static final ConcurrentHashMap<String, CircuitBreakerConfig> configCache =
      new ConcurrentHashMap<>();

  // Global flag for circuit breaker enablement
  private boolean circuitBreakerEnabled = false;

  public CircuitBreakerConfigProvider(Config config) {
    initialize(config);
  }

  public CircuitBreakerConfigProvider() {}

  /** Initializes and caches all CircuitBreaker configurations. */
  public void initialize(Config config) {
    if (!config.hasPath(CIRCUIT_BREAKER_CONFIG)) {
      log.warn("No circuit breaker configurations found in the config file.");
      return;
    }

    Config circuitBreakerConfig = config.getConfig(CIRCUIT_BREAKER_CONFIG);

    // Read global enabled flag (default to false if not provided)
    circuitBreakerEnabled =
        circuitBreakerConfig.hasPath(ENABLED) && circuitBreakerConfig.getBoolean(ENABLED);

    // Load all circuit breaker configurations and cache them
    Map<String, CircuitBreakerConfig> allConfigs =
        circuitBreakerConfig.root().keySet().stream()
            .filter(key -> !key.equals(ENABLED)) // Ignore the global enabled flag
            .collect(
                Collectors.toMap(
                    key -> key, // Circuit breaker key
                    key -> createCircuitBreakerConfig(circuitBreakerConfig.getConfig(key))));

    // Store in cache
    configCache.putAll(allConfigs);

    log.info(
        "Loaded {} circuit breaker configurations, Global Enabled: {}. Configs: {}",
        allConfigs.size(),
        circuitBreakerEnabled,
        allConfigs);
  }

  /**
   * Retrieves the CircuitBreakerConfig for a specific key. Falls back to default if key-specific
   * config is not found.
   */
  public CircuitBreakerConfig getConfig(String circuitBreakerKey) {
    return configCache.getOrDefault(circuitBreakerKey, configCache.get(DEFAULT_CONFIG_KEY));
  }

  /** Checks if Circuit Breaker is globally enabled. */
  public boolean isCircuitBreakerEnabled() {
    return circuitBreakerEnabled;
  }

  private CircuitBreakerConfig createCircuitBreakerConfig(Config config) {
    return CircuitBreakerConfig.custom()
        .failureRateThreshold((float) config.getDouble(FAILURE_RATE_THRESHOLD))
        .slowCallRateThreshold((float) config.getDouble(SLOW_CALL_RATE_THRESHOLD))
        .slowCallDurationThreshold(config.getDuration(SLOW_CALL_DURATION_THRESHOLD))
        .slidingWindowType(getSlidingWindowType(config.getString(SLIDING_WINDOW_TYPE)))
        .slidingWindowSize(config.getInt(SLIDING_WINDOW_SIZE))
        .waitDurationInOpenState(config.getDuration(WAIT_DURATION_IN_OPEN_STATE))
        .permittedNumberOfCallsInHalfOpenState(
            config.getInt(PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE))
        .minimumNumberOfCalls(config.getInt(MINIMUM_NUMBER_OF_CALLS))
        .build();
  }

  private CircuitBreakerConfig.SlidingWindowType getSlidingWindowType(String slidingWindowType) {
    return CircuitBreakerConfig.SlidingWindowType.valueOf(slidingWindowType);
  }
}
