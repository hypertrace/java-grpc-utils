package org.hypertrace.circuitbreaker.grpcutils;

import java.util.Map;
import java.util.function.BiFunction;
import lombok.Builder;
import lombok.Value;
import org.hypertrace.core.grpcutils.context.RequestContext;

@Value
@Builder
public class CircuitBreakerConfiguration<T> {
  Class<T> requestClass;
  BiFunction<RequestContext, T, String> keyFunction;
  boolean enabled;
  Map<String, CircuitBreakerThresholds> circuitBreakerThresholdsMap;
}
