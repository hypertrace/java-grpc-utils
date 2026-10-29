package org.hypertrace.circuitbreaker.grpcutils;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
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
import io.grpc.StatusRuntimeException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ServiceCircuitBreakerInterceptor implements ClientInterceptor {

  private final Map<String, CircuitBreakerConfig> circuitBreakerConfigs;
  private final Map<String, Set<Status.Code>> failureStatusCodesByService;
  private final CircuitBreakerRegistry circuitBreakerRegistry;

  static ClientInterceptor create(Map<String, CircuitBreakerThresholds> thresholdsByService) {
    if (thresholdsByService == null || thresholdsByService.isEmpty()) {
      return passthrough();
    }
    Map<String, CircuitBreakerConfig> circuitBreakerConfigs = new HashMap<>();
    Map<String, Set<Status.Code>> failureStatusCodesByService = new HashMap<>();
    for (Map.Entry<String, CircuitBreakerThresholds> entry : thresholdsByService.entrySet()) {
      CircuitBreakerThresholds thresholds = entry.getValue();
      if (!thresholds.isEnabled()) {
        log.debug("Service circuit breaker disabled for {}", entry.getKey());
        continue;
      }
      circuitBreakerConfigs.put(entry.getKey(), toCircuitBreakerConfig(thresholds));
      failureStatusCodesByService.put(entry.getKey(), thresholds.getFailureStatusCodes());
    }
    if (circuitBreakerConfigs.isEmpty()) {
      return passthrough();
    }
    return new ServiceCircuitBreakerInterceptor(
        Map.copyOf(circuitBreakerConfigs), Map.copyOf(failureStatusCodesByService));
  }

  private static ServiceCircuitBreakerInterceptor passthrough() {
    return new ServiceCircuitBreakerInterceptor(Map.of(), Map.of());
  }

  // Resilience4j records onError as a success when recordException returns false.
  private static CircuitBreakerConfig toCircuitBreakerConfig(CircuitBreakerThresholds thresholds) {
    final Set<Status.Code> failureStatusCodes = thresholds.getFailureStatusCodes();
    return CircuitBreakerConfig.custom()
        .failureRateThreshold(thresholds.getFailureRateThreshold())
        .slowCallRateThreshold(thresholds.getSlowCallRateThreshold())
        .slowCallDurationThreshold(thresholds.getSlowCallDurationThreshold())
        .slidingWindowType(slidingWindowType(thresholds.getSlidingWindowType()))
        .slidingWindowSize(thresholds.getSlidingWindowSize())
        .waitDurationInOpenState(thresholds.getWaitDurationInOpenState())
        .permittedNumberOfCallsInHalfOpenState(
            thresholds.getPermittedNumberOfCallsInHalfOpenState())
        .minimumNumberOfCalls(thresholds.getMinimumNumberOfCalls())
        .recordException(
            throwable ->
                throwable instanceof StatusRuntimeException
                    && failureStatusCodes.contains(
                        ((StatusRuntimeException) throwable).getStatus().getCode()))
        .build();
  }

  private static CircuitBreakerConfig.SlidingWindowType slidingWindowType(
      CircuitBreakerThresholds.SlidingWindowType slidingWindowType) {
    switch (slidingWindowType) {
      case COUNT_BASED:
        return CircuitBreakerConfig.SlidingWindowType.COUNT_BASED;
      case TIME_BASED:
      default:
        return CircuitBreakerConfig.SlidingWindowType.TIME_BASED;
    }
  }

  private ServiceCircuitBreakerInterceptor(
      Map<String, CircuitBreakerConfig> circuitBreakerConfigs,
      Map<String, Set<Status.Code>> failureStatusCodesByService) {
    this.circuitBreakerConfigs = circuitBreakerConfigs;
    this.failureStatusCodesByService = failureStatusCodesByService;
    this.circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
    final String serviceName = method.getServiceName();
    final CircuitBreakerConfig circuitBreakerConfig = circuitBreakerConfigs.get(serviceName);
    if (circuitBreakerConfig == null) {
      return next.newCall(method, callOptions);
    }

    final CircuitBreaker circuitBreaker =
        circuitBreakerRegistry.circuitBreaker(serviceName, circuitBreakerConfig);
    if (!circuitBreaker.tryAcquirePermission()) {
      log.debug("Circuit breaker is open for {}; rejecting the call", serviceName);
      throw Status.RESOURCE_EXHAUSTED
          .withDescription("Circuit breaker is open for " + serviceName)
          .asRuntimeException();
    }

    final ClientCall<ReqT, RespT> delegate;
    try {
      delegate = next.newCall(method, callOptions);
    } catch (RuntimeException exception) {
      circuitBreaker.releasePermission();
      throw exception;
    }
    return recordingCall(delegate, circuitBreaker, failureStatusCodesByService.get(serviceName));
  }

  private <ReqT, RespT> ClientCall<ReqT, RespT> recordingCall(
      ClientCall<ReqT, RespT> delegate,
      CircuitBreaker circuitBreaker,
      Set<Status.Code> failureStatusCodes) {
    return new ForwardingClientCall.SimpleForwardingClientCall<>(delegate) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        final long startedAtNanos = System.nanoTime();
        super.start(
            new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(
                responseListener) {
              @Override
              public void onClose(Status status, Metadata trailers) {
                final long elapsedNanos = System.nanoTime() - startedAtNanos;
                if (status.isOk() || !failureStatusCodes.contains(status.getCode())) {
                  log.trace(
                      "Recording success for {} after {} ns with status {}",
                      circuitBreaker.getName(),
                      elapsedNanos,
                      status.getCode());
                  circuitBreaker.onSuccess(elapsedNanos, TimeUnit.NANOSECONDS);
                } else {
                  log.debug(
                      "Recording failure for {} after {} ns with status {}",
                      circuitBreaker.getName(),
                      elapsedNanos,
                      status.getCode());
                  circuitBreaker.onError(
                      elapsedNanos, TimeUnit.NANOSECONDS, status.asRuntimeException());
                }
                super.onClose(status, trailers);
              }
            },
            headers);
      }
    };
  }
}
