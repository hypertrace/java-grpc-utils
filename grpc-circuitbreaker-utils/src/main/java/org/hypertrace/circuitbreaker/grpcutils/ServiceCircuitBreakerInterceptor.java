package org.hypertrace.circuitbreaker.grpcutils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigUtil;
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
import java.util.stream.Collectors;

public final class ServiceCircuitBreakerInterceptor implements ClientInterceptor {

  private static final String CONFIG_PATH = "circuit.breaker.config";
  private static final String SYNTHETIC_SERVICE_KEY = "service";

  private final Map<String, CircuitBreakerConfig> circuitBreakerConfigs;
  private final Map<String, Set<Status.Code>> failureStatusCodesByService;
  private final CircuitBreakerRegistry circuitBreakerRegistry;

  static ClientInterceptor create(Config rootConfig) {
    if (!rootConfig.hasPath(CONFIG_PATH)) {
      return passthrough();
    }
    Config config = rootConfig.getConfig(CONFIG_PATH);
    if (!config.hasPath(CircuitBreakerConfigParser.ENABLED)
        || !config.getBoolean(CircuitBreakerConfigParser.ENABLED)) {
      return passthrough();
    }
    return configured(config);
  }

  private static ServiceCircuitBreakerInterceptor passthrough() {
    return new ServiceCircuitBreakerInterceptor(Map.of(), Map.of());
  }

  private static ServiceCircuitBreakerInterceptor configured(Config config) {
    Config defaults = libraryDefaults();
    if (config.hasPath(CircuitBreakerConfigParser.DEFAULT_THRESHOLDS)) {
      defaults =
          config.getConfig(CircuitBreakerConfigParser.DEFAULT_THRESHOLDS).withFallback(defaults);
    }

    Map<String, CircuitBreakerConfig> circuitBreakerConfigs = new HashMap<>();
    Map<String, Set<Status.Code>> failureStatusCodesByService = new HashMap<>();
    for (String serviceName : config.root().keySet()) {
      if (CircuitBreakerConfigParser.ENABLED.equals(serviceName)
          || CircuitBreakerConfigParser.DEFAULT_THRESHOLDS.equals(serviceName)) {
        continue;
      }
      // getConfig(key) treats dots as path separators, so service names are read with joinPath.
      Config resolved = config.getConfig(ConfigUtil.joinPath(serviceName)).withFallback(defaults);
      CircuitBreakerThresholds thresholds = parseServiceThresholds(resolved);
      if (!thresholds.isEnabled()) {
        continue;
      }
      circuitBreakerConfigs.put(serviceName, toCircuitBreakerConfig(thresholds));
      failureStatusCodesByService.put(serviceName, thresholds.getFailureStatusCodes());
    }
    return new ServiceCircuitBreakerInterceptor(
        Map.copyOf(circuitBreakerConfigs), Map.copyOf(failureStatusCodesByService));
  }

  private static Config libraryDefaults() {
    CircuitBreakerThresholds defaults = CircuitBreakerThresholds.builder().build();
    String failureStatuses =
        defaults.getFailureStatusCodes().stream()
            .map(Status.Code::name)
            .collect(Collectors.joining(", "));
    return ConfigFactory.parseString(
        "failureRateThreshold = "
            + defaults.getFailureRateThreshold()
            + "\n"
            + "slowCallRateThreshold = "
            + defaults.getSlowCallRateThreshold()
            + "\n"
            + "slowCallDurationThreshold = "
            + defaults.getSlowCallDurationThreshold().toMillis()
            + "ms\n"
            + "slidingWindowType = "
            + defaults.getSlidingWindowType().name()
            + "\n"
            + "slidingWindowSize = "
            + defaults.getSlidingWindowSize()
            + "\n"
            + "waitDurationInOpenState = "
            + defaults.getWaitDurationInOpenState().toMillis()
            + "ms\n"
            + "minimumNumberOfCalls = "
            + defaults.getMinimumNumberOfCalls()
            + "\n"
            + "permittedNumberOfCallsInHalfOpenState = "
            + defaults.getPermittedNumberOfCallsInHalfOpenState()
            + "\n"
            + "enabled = "
            + defaults.isEnabled()
            + "\n"
            + "failureStatusCodes = ["
            + failureStatuses
            + "]\n");
  }

  private static CircuitBreakerThresholds parseServiceThresholds(Config resolvedServiceConfig) {
    return CircuitBreakerConfigParser.parseConfig(
            resolvedServiceConfig.atKey(SYNTHETIC_SERVICE_KEY))
        .build()
        .getCircuitBreakerThresholdsMap()
        .get(SYNTHETIC_SERVICE_KEY);
  }

  // onError is counted as a success when this predicate returns false.
  private static CircuitBreakerConfig toCircuitBreakerConfig(CircuitBreakerThresholds thresholds) {
    Set<Status.Code> failureStatusCodes = thresholds.getFailureStatusCodes();
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
    String serviceName = method.getServiceName();
    CircuitBreakerConfig circuitBreakerConfig = circuitBreakerConfigs.get(serviceName);
    if (circuitBreakerConfig == null) {
      return next.newCall(method, callOptions);
    }

    CircuitBreaker circuitBreaker =
        circuitBreakerRegistry.circuitBreaker(serviceName, circuitBreakerConfig);
    if (!circuitBreaker.tryAcquirePermission()) {
      throw Status.RESOURCE_EXHAUSTED
          .withDescription("Circuit breaker is open for " + serviceName)
          .asRuntimeException();
    }

    ClientCall<ReqT, RespT> delegate;
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
        long startedAtNanos = System.nanoTime();
        super.start(
            new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(
                responseListener) {
              @Override
              public void onClose(Status status, Metadata trailers) {
                long elapsedNanos = System.nanoTime() - startedAtNanos;
                if (status.isOk() || !failureStatusCodes.contains(status.getCode())) {
                  circuitBreaker.onSuccess(elapsedNanos, TimeUnit.NANOSECONDS);
                } else {
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
