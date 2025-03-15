package org.hypertrace.circuitbreaker.grpcutils.resilience;

import com.google.common.annotations.VisibleForTesting;
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
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerConfiguration;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerInterceptor;
import org.hypertrace.core.grpcutils.context.RequestContext;

@Slf4j
public class ResilienceCircuitBreakerInterceptor extends CircuitBreakerInterceptor {

  private final CircuitBreakerRegistry resilicenceCircuitBreakerRegistry;
  private final Map<String, CircuitBreakerConfig> resilienceCircuitBreakerConfigMap;
  private final ResilienceCircuitBreakerProvider resilienceCircuitBreakerProvider;
  private final CircuitBreakerConfiguration<?> circuitBreakerConfiguration;
  private final Clock clock;

  public ResilienceCircuitBreakerInterceptor(
      CircuitBreakerConfiguration<?> circuitBreakerConfiguration, Clock clock) {
    this.circuitBreakerConfiguration = circuitBreakerConfiguration;
    this.clock = clock;
    this.resilienceCircuitBreakerConfigMap =
        ResilienceCircuitBreakerConfigParser.getCircuitBreakerConfigs(
            circuitBreakerConfiguration.getCircuitBreakerThresholdsMap());
    this.resilicenceCircuitBreakerRegistry =
        new ResilienceCircuitBreakerRegistryProvider(resilienceCircuitBreakerConfigMap)
            .getCircuitBreakerRegistry();
    this.resilienceCircuitBreakerProvider =
        new ResilienceCircuitBreakerProvider(
            resilicenceCircuitBreakerRegistry, resilienceCircuitBreakerConfigMap);
  }

  @VisibleForTesting
  public ResilienceCircuitBreakerInterceptor(
      Clock clock,
      CircuitBreakerRegistry resilicenceCircuitBreakerRegistry,
      ResilienceCircuitBreakerProvider resilienceCircuitBreakerProvider,
      CircuitBreakerConfiguration<?> circuitBreakerConfiguration) {
    this.circuitBreakerConfiguration = circuitBreakerConfiguration;
    this.resilienceCircuitBreakerConfigMap =
        ResilienceCircuitBreakerConfigParser.getCircuitBreakerConfigs(
            circuitBreakerConfiguration.getCircuitBreakerThresholdsMap());
    this.resilicenceCircuitBreakerRegistry = resilicenceCircuitBreakerRegistry;
    this.resilienceCircuitBreakerProvider = resilienceCircuitBreakerProvider;
    this.clock = clock;
  }

  @Override
  protected boolean isCircuitBreakerEnabled() {
    return circuitBreakerConfiguration.isEnabled();
  }

  @Override
  protected <ReqT, RespT> ClientCall<ReqT, RespT> createInterceptedCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
    return new ForwardingClientCall.SimpleForwardingClientCall<>(
        next.newCall(method, callOptions)) {
      CircuitBreaker circuitBreaker;
      String circuitBreakerKey;

      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        Instant startTime = clock.instant();
        // Wrap response listener to track failures
        Listener<RespT> wrappedListener =
            wrapListenerWithCircuitBreaker(responseListener, startTime);
        super.start(wrappedListener, headers);
      }

      @SuppressWarnings("unchecked")
      @Override
      public void sendMessage(ReqT message) {
        CircuitBreakerConfiguration<ReqT> config =
            (CircuitBreakerConfiguration<ReqT>) circuitBreakerConfiguration;
        if (config.getRequestClass() == null
            || (!message.getClass().equals(config.getRequestClass()))) {
          log.warn("Invalid config for message type: {}", message.getClass());
          super.sendMessage(message);
        }
        if (config.getKeyFunction() != null) {
          circuitBreakerKey = config.getKeyFunction().apply(RequestContext.CURRENT.get(), message);
        } else {
          circuitBreakerKey = "default";
        }
        circuitBreaker = resilienceCircuitBreakerProvider.getCircuitBreaker(circuitBreakerKey);
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
