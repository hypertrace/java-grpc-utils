package org.hypertrace.circuitbreaker.grpcutils.resilience;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.grpc.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResilienceCircuitBreakerInterceptorTest {

  @Mock private Channel mockChannel;
  @Mock private ClientCall<Object, Object> mockClientCall;
  @Mock private CircuitBreaker mockCircuitBreaker;
  @Mock private Metadata mockMetadata;
  @Mock private ClientCall.Listener<Object> mockListener;
  @Mock private ResilienceCircuitBreakerRegistryProvider mockCircuitBreakerRegistryProvider;
  @Mock private ResilienceCircuitBreakerProvider mockCircuitBreakerProvider;
  @Mock private CircuitBreakerConfiguration<Object> mockCircuitBreakerConfig;
  @Mock private CircuitBreakerRegistry mockCircuitBreakerRegistry;

  @Mock private Clock fixedClock;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    fixedClock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
    when(mockChannel.newCall(any(), any())).thenReturn(mockClientCall);
  }

  @Test
  void testSendMessage_CallsSuperSendMessage_Success() {
    doNothing().when(mockClientCall).sendMessage(any());

    ResilienceCircuitBreakerInterceptor interceptor =
        new ResilienceCircuitBreakerInterceptor(
            mockCircuitBreakerConfig, fixedClock, mockCircuitBreakerProvider);

    ClientCall<Object, Object> interceptedCall =
        interceptor.createInterceptedCall(
            mock(MethodDescriptor.class), CallOptions.DEFAULT, mockChannel);

    interceptedCall.start(mockListener, mockMetadata);
    interceptedCall.sendMessage(new Object());

    verify(mockClientCall).sendMessage(any());
  }

  @Test
  void testSendMessage_CircuitBreakerRejectsRequest() {
    when(mockCircuitBreaker.tryAcquirePermission()).thenReturn(false);
    when(mockCircuitBreaker.getState()).thenReturn(CircuitBreaker.State.OPEN);
    when(mockCircuitBreakerProvider.getSharedCircuitBreaker())
        .thenReturn(Optional.of(mockCircuitBreaker));
    when(mockCircuitBreakerConfig.getExceptionBuilder())
        .thenReturn(
            reason ->
                new StatusRuntimeException(
                    Status.RESOURCE_EXHAUSTED.withDescription(reason), mock(Metadata.class)));
    ResilienceCircuitBreakerInterceptor interceptor =
        new ResilienceCircuitBreakerInterceptor(
            mockCircuitBreakerConfig, fixedClock, mockCircuitBreakerProvider);

    ClientCall<Object, Object> interceptedCall =
        interceptor.createInterceptedCall(
            mock(MethodDescriptor.class), CallOptions.DEFAULT, mockChannel);

    interceptedCall.start(mockListener, mockMetadata);

    assertThrows(
        StatusRuntimeException.class,
        () -> interceptedCall.sendMessage(new Object()),
        "Circuit Breaker should reject request");

    verify(mockClientCall, never()).sendMessage(any());
  }

  @Test
  void testSendMessage_CircuitBreakerInHalfOpenState() {
    when(mockCircuitBreaker.tryAcquirePermission()).thenReturn(false);
    when(mockCircuitBreaker.getState()).thenReturn(CircuitBreaker.State.HALF_OPEN);
    when(mockCircuitBreakerProvider.getSharedCircuitBreaker())
        .thenReturn(Optional.of(mockCircuitBreaker));
    when(mockCircuitBreakerConfig.getExceptionBuilder())
        .thenReturn(
            reason ->
                new StatusRuntimeException(
                    Status.RESOURCE_EXHAUSTED.withDescription(reason), mock(Metadata.class)));
    ResilienceCircuitBreakerInterceptor interceptor =
        new ResilienceCircuitBreakerInterceptor(
            mockCircuitBreakerConfig, fixedClock, mockCircuitBreakerProvider);

    ClientCall<Object, Object> interceptedCall =
        interceptor.createInterceptedCall(
            mock(MethodDescriptor.class), CallOptions.DEFAULT, mockChannel);

    interceptedCall.start(mockListener, mockMetadata);

    assertThrows(
        StatusRuntimeException.class,
        () -> interceptedCall.sendMessage(new Object()),
        "Circuit Breaker should reject requests when in HALF-OPEN state");

    verify(mockClientCall, never()).sendMessage(any());
  }

  @Test
  void testWrapListenerWithCircuitBreaker_Success() {
    when(mockCircuitBreaker.tryAcquirePermission()).thenReturn(true);
    when(mockCircuitBreakerProvider.getSharedCircuitBreaker())
        .thenReturn(Optional.of(mockCircuitBreaker));
    ResilienceCircuitBreakerInterceptor interceptor =
        new ResilienceCircuitBreakerInterceptor(
            mockCircuitBreakerConfig, fixedClock, mockCircuitBreakerProvider);

    ClientCall<Object, Object> interceptedCall =
        interceptor.createInterceptedCall(
            mock(MethodDescriptor.class), CallOptions.DEFAULT, mockChannel);

    interceptedCall.start(mockListener, mockMetadata);
    interceptedCall.sendMessage(new Object());

    // Trigger `onClose` directly to mimic gRPC's flow
    ArgumentCaptor<ForwardingClientCallListener<Object>> listenerCaptor =
        ArgumentCaptor.forClass(ForwardingClientCallListener.class);
    verify(mockClientCall).start(listenerCaptor.capture(), any());
    listenerCaptor.getValue().onClose(Status.OK, mockMetadata);

    verify(mockClientCall).sendMessage(any());
    verify(mockCircuitBreaker).onSuccess(anyLong(), eq(TimeUnit.NANOSECONDS));
  }

  @Test
  void testWrapListenerWithCircuitBreaker_Failure() {
    when(mockCircuitBreaker.tryAcquirePermission()).thenReturn(true);
    when(mockCircuitBreakerProvider.getSharedCircuitBreaker())
        .thenReturn(Optional.of(mockCircuitBreaker));
    ResilienceCircuitBreakerInterceptor interceptor =
        new ResilienceCircuitBreakerInterceptor(
            mockCircuitBreakerConfig, fixedClock, mockCircuitBreakerProvider);

    ClientCall<Object, Object> interceptedCall =
        interceptor.createInterceptedCall(
            mock(MethodDescriptor.class), CallOptions.DEFAULT, mockChannel);

    interceptedCall.start(mockListener, mockMetadata);
    interceptedCall.sendMessage(new Object());

    // Trigger `onClose` directly to mimic gRPC's flow
    ArgumentCaptor<ForwardingClientCallListener<Object>> listenerCaptor =
        ArgumentCaptor.forClass(ForwardingClientCallListener.class);
    verify(mockClientCall).start(listenerCaptor.capture(), any());
    listenerCaptor.getValue().onClose(Status.UNKNOWN, mockMetadata);

    verify(mockClientCall).sendMessage(any());
    verify(mockCircuitBreaker).onError(anyLong(), eq(TimeUnit.NANOSECONDS), any());
  }
}
