package org.hypertrace.circuitbreaker.grpcutils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.noop.NoopCounter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.hypertrace.core.serviceframework.metrics.PlatformMetricsRegistry;

public class CircuitBreakerMetricsNotifier {
  private static final ConcurrentHashMap<String, Counter> counterMap = new ConcurrentHashMap<>();
  public static final String UNKNOWN_TENANT = "unknown";

  public void incrementCount(String tenantId, String counterName) {
    getCounter(tenantId, counterName).increment();
  }

  public Counter getCounter(String tenantId, String counterName) {
    if (tenantId == null || tenantId.equals(UNKNOWN_TENANT)) {
      return getNoopCounter();
    }
    return counterMap.computeIfAbsent(
        tenantId + counterName,
        (unused) ->
            PlatformMetricsRegistry.registerCounter(counterName, Map.of("tenantId", tenantId)));
  }

  private NoopCounter getNoopCounter() {
    Meter.Id dummyId = new Meter.Id("noopCounter", Tags.empty(), null, null, Meter.Type.COUNTER);
    return new NoopCounter(dummyId);
  }
}
