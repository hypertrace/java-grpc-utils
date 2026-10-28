package org.hypertrace.circuitbreaker.grpcutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.hypertrace.core.grpcutils.client.GrpcChannelRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CircuitBreakerChannelRegistryTest {

  private static final String INSIGHTS_SERVICE =
      "ai.traceable.platform.insights.api.v1.InsightsService";
  private static final String ENABLED_INSIGHTS =
      "circuit.breaker.config {\n"
          + "  enabled = true\n"
          + "  \""
          + INSIGHTS_SERVICE
          + "\" {\n"
          + "    slowCallDurationThreshold = 1h\n"
          + "    minimumNumberOfCalls = 1\n"
          + "    failureRateThreshold = 1\n"
          + "    slidingWindowSize = 1\n"
          + "    slidingWindowType = COUNT_BASED\n"
          + "    waitDurationInOpenState = 60s\n"
          + "  }\n"
          + "}\n";

  @Test
  void nullConfigReturnsTheSameRegistry() {
    GrpcChannelRegistry registry = mock(GrpcChannelRegistry.class);

    assertSame(registry, CircuitBreakerChannelRegistry.wrap(registry, null));
  }

  @Test
  void emptyConfigReturnsTheSameRegistry() {
    GrpcChannelRegistry registry = mock(GrpcChannelRegistry.class);

    assertSame(registry, CircuitBreakerChannelRegistry.wrap(registry, ConfigFactory.empty()));
  }

  @Test
  void disabledConfigReturnsTheSameRegistry() {
    GrpcChannelRegistry registry = mock(GrpcChannelRegistry.class);
    Config config = ConfigFactory.parseString("circuit.breaker.config { enabled = false }");

    assertSame(registry, CircuitBreakerChannelRegistry.wrap(registry, config));
  }

  @Test
  void missingEnabledReturnsTheSameRegistry() {
    GrpcChannelRegistry registry = mock(GrpcChannelRegistry.class);
    Config config = ConfigFactory.parseString("circuit.breaker.config { }");

    assertSame(registry, CircuitBreakerChannelRegistry.wrap(registry, config));
  }

  @Test
  void nullRegistryIsReturnedUnchanged() {
    assertSame(null, CircuitBreakerChannelRegistry.wrap(null, ConfigFactory.empty()));
  }

  @Test
  void enabledConfigOpensTheBreakerOnUnavailable() {
    GrpcChannelRegistry registry = mock(GrpcChannelRegistry.class);
    ManagedChannel rawChannel = mock(ManagedChannel.class);
    when(registry.forPlaintextAddress("host", 80)).thenReturn(rawChannel);

    GrpcChannelRegistry wrapped =
        CircuitBreakerChannelRegistry.wrap(registry, ConfigFactory.parseString(ENABLED_INSIGHTS));
    assertNotSame(registry, wrapped);
    ManagedChannel channel = wrapped.forPlaintextAddress("host", 80);

    completeCall(channel, rawChannel, Status.UNAVAILABLE);

    StatusRuntimeException rejected =
        assertThrows(
            StatusRuntimeException.class, () -> channel.newCall(method(), CallOptions.DEFAULT));
    assertEquals(Status.Code.RESOURCE_EXHAUSTED, rejected.getStatus().getCode());
    verify(rawChannel, times(1)).newCall(any(), any());
  }

  @Test
  void repeatedLookupReturnsTheSameInterceptedChannel() {
    GrpcChannelRegistry registry = mock(GrpcChannelRegistry.class);
    ManagedChannel rawChannel = mock(ManagedChannel.class);
    when(registry.forPlaintextAddress("host", 80)).thenReturn(rawChannel);

    GrpcChannelRegistry wrapped =
        CircuitBreakerChannelRegistry.wrap(registry, ConfigFactory.parseString(ENABLED_INSIGHTS));

    assertSame(wrapped.forPlaintextAddress("host", 80), wrapped.forPlaintextAddress("host", 80));
  }

  @SuppressWarnings("unchecked")
  private static void completeCall(ManagedChannel channel, ManagedChannel raw, Status status) {
    ClientCall<Object, Object> delegateCall = mock(ClientCall.class);
    when(raw.newCall(any(), any())).thenReturn((ClientCall) delegateCall);
    ClientCall.Listener<Object> listener = mock(ClientCall.Listener.class);

    ClientCall<Object, Object> call = channel.newCall(method(), CallOptions.DEFAULT);
    call.start(listener, new Metadata());

    ArgumentCaptor<ClientCall.Listener<Object>> captor =
        ArgumentCaptor.forClass(ClientCall.Listener.class);
    verify(delegateCall).start(captor.capture(), any());
    captor.getValue().onClose(status, new Metadata());
  }

  @SuppressWarnings("unchecked")
  private static MethodDescriptor<Object, Object> method() {
    MethodDescriptor<Object, Object> method = mock(MethodDescriptor.class);
    when(method.getServiceName()).thenReturn(INSIGHTS_SERVICE);
    return method;
  }
}
