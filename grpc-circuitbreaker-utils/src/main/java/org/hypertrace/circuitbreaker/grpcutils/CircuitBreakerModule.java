package org.hypertrace.circuitbreaker.grpcutils;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CircuitBreakerModule extends AbstractModule {

  private final Config config;

  public CircuitBreakerModule(Config config) {
    this.config = config;
  }

  @Provides
  @Singleton
  public CircuitBreakerMetricsNotifier providesCircuitBreakerMetricsNotifier() {
    return new CircuitBreakerMetricsNotifier();
  }

  @Provides
  @Singleton
  public CircuitBreakerConfigProvider providesCircuitBreakerConfigProvider() {
    return new CircuitBreakerConfigProvider(config);
  }

  @Provides
  @Singleton
  public CircuitBreakerRegistry providesCircuitBreakerRegistry(
      CircuitBreakerConfigProvider circuitBreakerConfigProvider) {
    if (!circuitBreakerConfigProvider.isCircuitBreakerEnabled()) {
      return CircuitBreakerRegistry.ofDefaults();
    }
    CircuitBreakerRegistry circuitBreakerRegistry =
        CircuitBreakerRegistry.of(
            circuitBreakerConfigProvider.getConfig(
                CircuitBreakerConfigProvider.DEFAULT_CONFIG_KEY));
    circuitBreakerRegistry
        .getEventPublisher()
        .onEntryAdded(
            entryAddedEvent -> {
              CircuitBreaker addedCircuitBreaker = entryAddedEvent.getAddedEntry();
              log.info("CircuitBreaker {} added", addedCircuitBreaker.getName());
            })
        .onEntryRemoved(
            entryRemovedEvent -> {
              CircuitBreaker removedCircuitBreaker = entryRemovedEvent.getRemovedEntry();
              log.info("CircuitBreaker {} removed", removedCircuitBreaker.getName());
            });
    return circuitBreakerRegistry;
  }

  @Provides
  @Singleton
  public CircuitBreakerInterceptor providesCircuitBreakerInterceptor(
      CircuitBreakerRegistry circuitBreakerRegistry,
      CircuitBreakerConfigProvider circuitBreakerConfigProvider,
      CircuitBreakerMetricsNotifier circuitBreakerMetricsNotifier) {
    return new CircuitBreakerInterceptor(
        circuitBreakerRegistry, circuitBreakerConfigProvider, circuitBreakerMetricsNotifier);
  }
}
