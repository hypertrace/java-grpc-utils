package org.hypertrace.circuitbreaker.grpcutils;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CircuitBreakerInterceptor implements ClientInterceptor {

  public static final CallOptions.Key<String> CIRCUIT_BREAKER_KEY =
      CallOptions.Key.createWithDefault("circuitBreakerKey", "default");
  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final CircuitBreakerConfigProvider circuitBreakerConfigProvider;
  private final CircuitBreakerMetricsNotifier circuitBreakerMetricsNotifier;

  public CircuitBreakerInterceptor(
      CircuitBreakerRegistry circuitBreakerRegistry,
      CircuitBreakerConfigProvider circuitBreakerConfigProvider,
      CircuitBreakerMetricsNotifier circuitBreakerMetricsNotifier) {
    this.circuitBreakerRegistry = circuitBreakerRegistry;
    this.circuitBreakerConfigProvider = circuitBreakerConfigProvider;
    this.circuitBreakerMetricsNotifier = circuitBreakerMetricsNotifier;
  }

  // Intercepts the call and applies circuit breaker logic
  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
    if (!circuitBreakerConfigProvider.isCircuitBreakerEnabled()) {
      return next.newCall(method, callOptions);
    }

    // Get circuit breaker key from CallOptions
    String circuitBreakerKey = callOptions.getOption(CIRCUIT_BREAKER_KEY);
    CircuitBreaker circuitBreaker = getCircuitBreaker(circuitBreakerKey);
    return new ForwardingClientCall.SimpleForwardingClientCall<>(
        next.newCall(method, callOptions)) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        long startTime = System.nanoTime();

        // Wrap response listener to track failures
        Listener<RespT> wrappedListener =
            new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(
                responseListener) {
              @Override
              public void onClose(Status status, Metadata trailers) {
                long duration = System.nanoTime() - startTime;
                if (status.isOk()) {
                  circuitBreaker.onSuccess(duration, TimeUnit.NANOSECONDS);
                } else {
                  log.debug(
                      "Circuit Breaker '{}' detected failure. Status: {}, Description: {}",
                      circuitBreaker.getName(),
                      status.getCode(),
                      status.getDescription());
                  circuitBreaker.onError(
                      duration, TimeUnit.NANOSECONDS, status.asRuntimeException());
                }
                super.onClose(status, trailers);
              }
            };

        super.start(wrappedListener, headers);
      }

      @Override
      public void sendMessage(ReqT message) {
        if (!circuitBreaker.tryAcquirePermission()) {
          handleCircuitBreakerRejection(circuitBreakerKey, circuitBreaker);
          String rejectionReason =
              circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN
                  ? "Circuit Breaker is HALF-OPEN and rejecting excess requests"
                  : "Circuit Breaker is OPEN and blocking requests";
          throw Status.UNAVAILABLE.withDescription(rejectionReason).asRuntimeException();
        }
        super.sendMessage(message);
      }
    };
  }

  private void handleCircuitBreakerRejection(
      String circuitBreakerKey, CircuitBreaker circuitBreaker) {
    String tenantId = getTenantId(circuitBreakerKey);
    if (circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN) {
      circuitBreakerMetricsNotifier.incrementCount(tenantId, "circuitbreaker.halfopen.rejected");
      log.debug(
          "Circuit Breaker '{}' is HALF-OPEN and rejecting excess requests for tenant '{}'.",
          circuitBreakerKey,
          tenantId);
    } else if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
      circuitBreakerMetricsNotifier.incrementCount(tenantId, "circuitbreaker.open.blocked");
      log.debug(
          "Circuit Breaker '{}' is OPEN. Blocking request for tenant '{}'.",
          circuitBreakerKey,
          tenantId);
    } else {
      log.debug( // Added unexpected state handling for safety
          "Unexpected Circuit Breaker state '{}' for '{}'. Blocking request.",
          circuitBreaker.getState(),
          circuitBreakerKey);
    }
  }

  private static String getTenantId(String circuitBreakerKey) {
    if (!circuitBreakerKey.contains(".")) {
      return "Unknown";
    }
    return circuitBreakerKey.split("\\.", 2)[0]; // Ensures only the first split
  }

  /** Retrieve the Circuit Breaker based on the key. */
  private CircuitBreaker getCircuitBreaker(String circuitBreakerKey) {
    CircuitBreaker circuitBreaker =
        circuitBreakerRegistry.circuitBreaker(
            circuitBreakerKey, circuitBreakerConfigProvider.getConfig(circuitBreakerKey));
    CircuitBreakerEventListener.attachListeners(circuitBreaker);
    return circuitBreaker;
  }
}
