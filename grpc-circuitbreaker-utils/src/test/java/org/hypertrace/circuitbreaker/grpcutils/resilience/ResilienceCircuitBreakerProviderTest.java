package org.hypertrace.circuitbreaker.grpcutils.resilience;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResilienceCircuitBreakerProviderTest {

  @Mock private CircuitBreakerRegistry circuitBreakerRegistry;
  @Mock private CircuitBreaker mockCircuitBreaker;
  @Mock private CircuitBreaker.EventPublisher eventPublisher;

  private ResilienceCircuitBreakerProvider provider;
  private Map<String, CircuitBreakerConfig> configMap;
  private List<String> disabledKeys;
  private static final String CONFIG_KEY = "defaultConfig";
  private static final String ENABLED_SERVICE = "enabledService";
  private static final String DISABLED_SERVICE = "disabledService";
  private static final String NON_CONFIGURED_SERVICE = "nonConfiguredService";
  private CircuitBreakerConfig defaultConfig;
  private CircuitBreakerConfig serviceConfig;

  @BeforeEach
  void setup() {
    defaultConfig = CircuitBreakerConfig.custom().build();
    serviceConfig = CircuitBreakerConfig.custom().build();

    configMap = new HashMap<>();
    configMap.put(ENABLED_SERVICE, serviceConfig);
    configMap.put(CONFIG_KEY, defaultConfig);

    disabledKeys = Arrays.asList(DISABLED_SERVICE);

    // Use lenient() for all mocks since they might not be used in every test
    lenient().when(mockCircuitBreaker.getEventPublisher()).thenReturn(eventPublisher);
    lenient().when(eventPublisher.onStateTransition(any())).thenReturn(eventPublisher);
    lenient().when(eventPublisher.onCallNotPermitted(any())).thenReturn(eventPublisher);
    lenient().doNothing().when(eventPublisher).onEvent(any());

    lenient().when(circuitBreakerRegistry.circuitBreaker(eq(ENABLED_SERVICE), eq(serviceConfig)))
        .thenReturn(mockCircuitBreaker);
    lenient().when(circuitBreakerRegistry.circuitBreaker(eq(NON_CONFIGURED_SERVICE), eq(defaultConfig)))
        .thenReturn(mockCircuitBreaker);

    provider = new ResilienceCircuitBreakerProvider(
        circuitBreakerRegistry,
        configMap,
        disabledKeys,
        true,
        CONFIG_KEY);
  }

  @Test
  void shouldReturnCircuitBreakerForEnabledKey() {
    Optional<CircuitBreaker> result = provider.getCircuitBreaker(ENABLED_SERVICE);
    assertTrue(result.isPresent());
    assertEquals(mockCircuitBreaker, result.get());

    // Verify cache behavior by getting the same key again
    Optional<CircuitBreaker> secondResult = provider.getCircuitBreaker(ENABLED_SERVICE);
    assertTrue(secondResult.isPresent());
    assertEquals(mockCircuitBreaker, secondResult.get());

    // Verify circuit breaker was created only once
    verify(circuitBreakerRegistry, times(1))
        .circuitBreaker(eq(ENABLED_SERVICE), eq(serviceConfig));
  }

  @Test
  void shouldReturnEmptyForDisabledKey() {
    Optional<CircuitBreaker> result = provider.getCircuitBreaker(DISABLED_SERVICE);
    assertFalse(result.isPresent());
  }

  @Test
  void shouldReturnEmptyForNonConfiguredServiceWhenConfigKeyDisabled() {
    // Create provider with disabled config key
    ResilienceCircuitBreakerProvider providerWithDisabledConfig = new ResilienceCircuitBreakerProvider(
        circuitBreakerRegistry,
        configMap,
        Arrays.asList(DISABLED_SERVICE, CONFIG_KEY), // CONFIG_KEY is disabled
        true,
        CONFIG_KEY);

    Optional<CircuitBreaker> result = providerWithDisabledConfig.getCircuitBreaker(NON_CONFIGURED_SERVICE);
    assertFalse(result.isPresent());
  }

  @Test
  void shouldReturnCircuitBreakerForNonConfiguredServiceWhenConfigKeyEnabled() {
    Optional<CircuitBreaker> result = provider.getCircuitBreaker(NON_CONFIGURED_SERVICE);
    assertTrue(result.isPresent());
    assertEquals(mockCircuitBreaker, result.get());

    // Verify it used the default config
    verify(circuitBreakerRegistry, times(1))
        .circuitBreaker(eq(NON_CONFIGURED_SERVICE), eq(defaultConfig));
  }

  @Test
  void shouldCacheCircuitBreakerInstances() {
    // Get the same circuit breaker multiple times
    provider.getCircuitBreaker(ENABLED_SERVICE);
    provider.getCircuitBreaker(ENABLED_SERVICE);
    provider.getCircuitBreaker(ENABLED_SERVICE);

    // Verify circuit breaker was created only once despite multiple calls
    verify(circuitBreakerRegistry, times(1))
        .circuitBreaker(eq(ENABLED_SERVICE), eq(serviceConfig));
  }

  @Test
  void shouldHandleAllBuildNewCircuitBreakerScenarios() {
    // Case 1: Service with specific config against circuit breaker key
    Optional<CircuitBreaker> serviceSpecificResult = provider.getCircuitBreaker(ENABLED_SERVICE);
    assertTrue(serviceSpecificResult.isPresent());
    assertEquals(mockCircuitBreaker, serviceSpecificResult.get());
    verify(circuitBreakerRegistry).circuitBreaker(ENABLED_SERVICE, serviceConfig);

    // Case 2: Service without specific config against circuit breaker key but with config present against config key
    String fallbackService = "fallbackService";
    lenient().when(circuitBreakerRegistry.circuitBreaker(eq(fallbackService), eq(defaultConfig)))
        .thenReturn(mockCircuitBreaker);
    Optional<CircuitBreaker> fallbackResult = provider.getCircuitBreaker(fallbackService);
    assertTrue(fallbackResult.isPresent());
    assertEquals(mockCircuitBreaker, fallbackResult.get());
    verify(circuitBreakerRegistry).circuitBreaker(fallbackService, defaultConfig);

    // Case 3: Service with no config present against circuit breaker key and no config key, but defaultEnabled=true
    String noConfigService = "noConfigService";
    ResilienceCircuitBreakerProvider defaultEnabledProvider = new ResilienceCircuitBreakerProvider(
        circuitBreakerRegistry,
        configMap,
        disabledKeys,
        true, // defaultEnabled = true
        null); // no config key

    lenient().when(circuitBreakerRegistry.circuitBreaker(eq(noConfigService)))
        .thenReturn(mockCircuitBreaker);
    Optional<CircuitBreaker> noConfigResult = defaultEnabledProvider.getCircuitBreaker(noConfigService);
    assertTrue(noConfigResult.isPresent());
    assertEquals(mockCircuitBreaker, noConfigResult.get());
    verify(circuitBreakerRegistry).circuitBreaker(noConfigService);

    // Case 4: Service with no config present against circuit breaker key and no config key, and defaultEnabled=false
    ResilienceCircuitBreakerProvider disabledDefaultProvider = new ResilienceCircuitBreakerProvider(
        circuitBreakerRegistry,
        configMap,
        disabledKeys,
        false, // defaultEnabled = false
        null); // no config key

    Optional<CircuitBreaker> disabledDefaultResult = disabledDefaultProvider.getCircuitBreaker(noConfigService);
    assertFalse(disabledDefaultResult.isPresent());
  }
}
