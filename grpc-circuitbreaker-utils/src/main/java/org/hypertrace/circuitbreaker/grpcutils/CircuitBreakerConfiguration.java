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
  @Builder.Default boolean enabled = false;
  // Default value be "global" if not override.
  @Builder.Default String defaultCircuitBreakerKey = "global";
  // Standard/default thresholds
  CircuitBreakerThresholds defaultThresholds;
  // Custom overrides for specific cases (less common)
  @Builder.Default Map<String, CircuitBreakerThresholds> circuitBreakerThresholdsMap = Map.of();
}
