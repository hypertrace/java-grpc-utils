package org.hypertrace.circuitbreaker.grpcutils.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerConfiguration;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerInterceptor;
import org.hypertrace.core.grpcutils.context.RequestContext;

@Slf4j
class ResilienceCircuitBreakerInterceptor extends CircuitBreakerInterceptor {

  private final ResilienceCircuitBreakerProvider resilienceCircuitBreakerProvider;
  private final CircuitBreakerConfiguration<?> circuitBreakerConfiguration;
  private final Clock clock;

  ResilienceCircuitBreakerInterceptor(
      CircuitBreakerConfiguration<?> circuitBreakerConfiguration,
      Clock clock,
      ResilienceCircuitBreakerProvider resilienceCircuitBreakerProvider) {
    this.circuitBreakerConfiguration = circuitBreakerConfiguration;
    this.clock = clock;
    this.resilienceCircuitBreakerProvider = resilienceCircuitBreakerProvider;
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
      Optional<CircuitBreaker> optionalCircuitBreaker;

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
        // Type check for message class compatibility
        if (config.getRequestClass() != null && !config.getRequestClass().isInstance(message)) {
          super.sendMessage(message);
          return;
        }
        String circuitBreakerKey = null;
        if (config.getKeyFunction() != null) {
          circuitBreakerKey = config.getKeyFunction().apply(RequestContext.CURRENT.get(), message);
        }
        optionalCircuitBreaker =
            circuitBreakerKey != null
                ? resilienceCircuitBreakerProvider.getCircuitBreaker(circuitBreakerKey)
                : resilienceCircuitBreakerProvider.getSharedCircuitBreaker();

        CircuitBreaker circuitBreaker = optionalCircuitBreaker.orElse(null);
        if (circuitBreaker == null) {
          super.sendMessage(message);
          return;
        }
        if (!circuitBreaker.tryAcquirePermission()) {
          logCircuitBreakerRejection(circuitBreakerKey, circuitBreaker);
          String rejectionReason =
              circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN
                  ? "Circuit Breaker is HALF-OPEN and rejecting excess requests"
                  : "Circuit Breaker is OPEN and blocking requests";
          throw config.getExceptionBuilder().apply(rejectionReason);
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
            CircuitBreaker circuitBreaker = optionalCircuitBreaker.orElse(null);
            if (circuitBreaker == null) {
              super.onClose(status, trailers);
              return;
            }
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
