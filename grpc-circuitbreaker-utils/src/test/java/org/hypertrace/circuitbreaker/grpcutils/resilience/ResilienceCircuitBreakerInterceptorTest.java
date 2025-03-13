package org.hypertrace.circuitbreaker.grpcutils.resilience;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.StatusRuntimeException;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ResilienceCircuitBreakerInterceptorTest {

  private final Config config =
      ConfigFactory.parseString(
          "enabled=true\n"
              + "default {\n"
              + "  failureRateThreshold=50.0\n"
              + "  slowCallRateThreshold=100.0\n"
              + "  slowCallDurationThreshold=5s\n"
              + "  slidingWindowSize=10\n"
              + "  waitDurationInOpenState=1m\n"
              + "  minimumNumberOfCalls=5\n"
              + "  permittedNumberOfCallsInHalfOpenState=3\n"
              + "  slidingWindowType=COUNT_BASED\n"
              + "}");
  private final Clock clock = Clock.systemUTC();
  private final CircuitBreakerRegistry mockRegistry = Mockito.mock(CircuitBreakerRegistry.class);
  private final CircuitBreaker mockCircuitBreaker = Mockito.mock(CircuitBreaker.class);
  private final Channel mockChannel = Mockito.mock(Channel.class);
  private final ClientCall.Listener<Object> mockListener = mock(ClientCall.Listener.class);
  private final ResilienceCircuitBreakerProvider mockCircuitBreakerProvider =
      Mockito.mock(ResilienceCircuitBreakerProvider.class);

  @Test
  void testCircuitBreakerEnabled_InterceptsCall() {
    MethodDescriptor<Object, Object> methodDescriptor = mock(MethodDescriptor.class);
    CallOptions callOptions =
        CallOptions.DEFAULT.withOption(
            ResilienceCircuitBreakerInterceptor.CIRCUIT_BREAKER_KEY, "test-key");
    when(mockCircuitBreakerProvider.getCircuitBreaker("test-key")).thenReturn(mockCircuitBreaker);
    ResilienceCircuitBreakerInterceptor interceptor =
        new ResilienceCircuitBreakerInterceptor(
            config, clock, mockRegistry, mockCircuitBreakerProvider);

    ClientCall<Object, Object> interceptedCall =
        spy(interceptor.interceptCall(methodDescriptor, callOptions, mockChannel));
    doNothing().when(interceptedCall).start(any(), any());
    assertNotNull(interceptedCall);
    assertDoesNotThrow(() -> interceptedCall.start(mockListener, new Metadata()));
    verify(interceptedCall).start(eq(mockListener), any(Metadata.class));
  }

  @Test
  void testCircuitBreakerRejectsRequest() {
    MethodDescriptor<Object, Object> methodDescriptor = mock(MethodDescriptor.class);
    CallOptions callOptions =
        CallOptions.DEFAULT.withOption(
            ResilienceCircuitBreakerInterceptor.CIRCUIT_BREAKER_KEY, "test-key");
    when(mockCircuitBreaker.tryAcquirePermission()).thenReturn(false);
    when(mockCircuitBreaker.getState()).thenReturn(CircuitBreaker.State.OPEN);
    when(mockCircuitBreakerProvider.getCircuitBreaker("test-key")).thenReturn(mockCircuitBreaker);
    ResilienceCircuitBreakerInterceptor interceptor =
        new ResilienceCircuitBreakerInterceptor(
            config, clock, mockRegistry, mockCircuitBreakerProvider);

    ClientCall<Object, Object> interceptedCall =
        interceptor.interceptCall(methodDescriptor, callOptions, mockChannel);
    assertThrows(StatusRuntimeException.class, () -> interceptedCall.sendMessage(new Object()));
  }

  @Test
  void testCircuitBreakerSuccess() {
    MethodDescriptor<Object, Object> methodDescriptor = mock(MethodDescriptor.class);
    CallOptions callOptions =
        CallOptions.DEFAULT.withOption(
            ResilienceCircuitBreakerInterceptor.CIRCUIT_BREAKER_KEY, "test-key");
    when(mockCircuitBreaker.tryAcquirePermission()).thenReturn(true);
    when(mockCircuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);
    when(mockCircuitBreakerProvider.getCircuitBreaker("test-key")).thenReturn(mockCircuitBreaker);
    ResilienceCircuitBreakerInterceptor interceptor =
        spy(
            new ResilienceCircuitBreakerInterceptor(
                config, clock, mockRegistry, mockCircuitBreakerProvider));
    ClientCall<Object, Object> interceptedCall =
        spy(interceptor.createInterceptedCall(methodDescriptor, callOptions, mockChannel));
    Mockito.doNothing().when((ForwardingClientCall) interceptedCall).sendMessage(Mockito.any());
    interceptedCall.sendMessage(new Object());
    // Assert
    verify(interceptedCall, times(1)).sendMessage(any());
  }
}
