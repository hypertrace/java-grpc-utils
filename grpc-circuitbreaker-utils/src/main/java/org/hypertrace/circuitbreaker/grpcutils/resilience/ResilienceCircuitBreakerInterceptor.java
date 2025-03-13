package org.hypertrace.circuitbreaker.grpcutils.resilience;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerConfigProvider;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerInterceptor;

@Slf4j
@Singleton
public class ResilienceCircuitBreakerInterceptor extends CircuitBreakerInterceptor {

  public static final CallOptions.Key<String> CIRCUIT_BREAKER_KEY =
      CallOptions.Key.createWithDefault("circuitBreakerKey", "default");
  private final CircuitBreakerRegistry resilicenceCircuitBreakerRegistry;
  private final CircuitBreakerConfigProvider circuitBreakerConfigProvider;
  private final Map<String, CircuitBreakerConfig> resilienceCircuitBreakerConfig;
  private final ResilienceCircuitBreakerProvider resilienceCircuitBreakerProvider;
  private final Clock clock;

  public ResilienceCircuitBreakerInterceptor(Config config, Clock clock) {
    this.circuitBreakerConfigProvider = new CircuitBreakerConfigProvider(config);
    this.resilienceCircuitBreakerConfig =
        ResilienceCircuitBreakerConfigParser.getCircuitBreakerConfigs(
            circuitBreakerConfigProvider.getConfigMap());
    this.resilicenceCircuitBreakerRegistry =
        new ResilienceCircuitBreakerRegistryProvider(resilienceCircuitBreakerConfig)
            .getCircuitBreakerRegistry();
    this.resilienceCircuitBreakerProvider =
        new ResilienceCircuitBreakerProvider(
            resilicenceCircuitBreakerRegistry, resilienceCircuitBreakerConfig);
    this.clock = clock;
  }

  @VisibleForTesting
  public ResilienceCircuitBreakerInterceptor(
      Config config,
      Clock clock,
      CircuitBreakerRegistry resilicenceCircuitBreakerRegistry,
      ResilienceCircuitBreakerProvider resilienceCircuitBreakerProvider) {
    this.circuitBreakerConfigProvider = new CircuitBreakerConfigProvider(config);
    this.resilienceCircuitBreakerConfig =
        ResilienceCircuitBreakerConfigParser.getCircuitBreakerConfigs(
            circuitBreakerConfigProvider.getConfigMap());
    this.resilicenceCircuitBreakerRegistry = resilicenceCircuitBreakerRegistry;
    this.resilienceCircuitBreakerProvider = resilienceCircuitBreakerProvider;
    this.clock = clock;
  }

  @Override
  protected boolean isCircuitBreakerEnabled() {
    return circuitBreakerConfigProvider.isCircuitBreakerEnabled();
  }

  @Override
  protected <ReqT, RespT> ClientCall<ReqT, RespT> createInterceptedCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
    // Get circuit breaker key from CallOptions
    String circuitBreakerKey = callOptions.getOption(CIRCUIT_BREAKER_KEY);
    CircuitBreaker circuitBreaker =
        resilienceCircuitBreakerProvider.getCircuitBreaker(circuitBreakerKey);
    return new ForwardingClientCall.SimpleForwardingClientCall<>(
        next.newCall(method, callOptions)) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        Instant startTime = clock.instant();
        // Wrap response listener to track failures
        Listener<RespT> wrappedListener =
            wrapListenerWithCircuitBreaker(responseListener, startTime);
        super.start(wrappedListener, headers);
      }

      @Override
      public void sendMessage(ReqT message) {
        if (!circuitBreaker.tryAcquirePermission()) {
          logCircuitBreakerRejection(circuitBreakerKey, circuitBreaker);
          String rejectionReason =
              circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN
                  ? "Circuit Breaker is HALF-OPEN and rejecting excess requests"
                  : "Circuit Breaker is OPEN and blocking requests";
          throw Status.RESOURCE_EXHAUSTED.withDescription(rejectionReason).asRuntimeException();
        }
        super.sendMessage(message);
      }

      private ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>
          wrapListenerWithCircuitBreaker(Listener<RespT> responseListener, Instant startTime) {
        return new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(
            responseListener) {
          @Override
          public void onClose(Status status, Metadata trailers) {
            long duration = Duration.between(startTime, clock.instant()).toNanos();
            if (status.isOk()) {
              circuitBreaker.onSuccess(duration, TimeUnit.NANOSECONDS);
            } else {
              log.debug(
                  "Circuit Breaker '{}' detected failure. Status: {}, Description: {}",
                  circuitBreaker.getName(),
                  status.getCode(),
                  status.getDescription());
              circuitBreaker.onError(duration, TimeUnit.NANOSECONDS, status.asRuntimeException());
            }
            super.onClose(status, trailers);
          }
        };
      }
    };
  }

  private void logCircuitBreakerRejection(String circuitBreakerKey, CircuitBreaker circuitBreaker) {
    Map<CircuitBreaker.State, String> stateMessages =
        Map.of(
            CircuitBreaker.State.HALF_OPEN, "is HALF-OPEN and rejecting excess requests.",
            CircuitBreaker.State.OPEN, "is OPEN and blocking requests");
    log.debug(
        "Circuit Breaker '{}' {}",
        circuitBreakerKey,
        stateMessages.getOrDefault(circuitBreaker.getState(), "is in an unexpected state"));
  }
}
