package org.hypertrace.ratelimiter.grpcutils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import io.grpc.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RateLimiterInterceptorTest {

  @Mock private ServerCall<String, String> mockServerCall;
  @Mock private Metadata mockMetadata;
  @Mock private ServerCallHandler<String, String> mockHandler;
  @Mock private ServerCall.Listener<String> mockListener;
  @Mock private RateLimiter mockRateLimiter;
  @Mock private RateLimiterFactory mockRateLimiterFactory;

  private static final String TEST_METHOD = "org.example.TestService/TestMethod";

  private RateLimiterInterceptor rateLimiterInterceptor;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Mock the ServerCallHandler to return the mock listener
    when(mockHandler.startCall(any(), any())).thenReturn(mockListener);

    // Mock the ServerCall's method descriptor
    when(mockServerCall.getMethodDescriptor())
        .thenReturn(
            MethodDescriptor.<String, String>newBuilder()
                .setFullMethodName(TEST_METHOD)
                .setType(MethodDescriptor.MethodType.UNARY)
                .setRequestMarshaller(mock(MethodDescriptor.Marshaller.class))
                .setResponseMarshaller(mock(MethodDescriptor.Marshaller.class))
                .build());

    // Initialize RateLimiterInterceptor with a sample configuration
    rateLimiterInterceptor =
        new RateLimiterInterceptor(
            List.of(
                RateLimiterConfiguration.builder()
                    .method(TEST_METHOD)
                    .matchAttributes(Map.of("tenant_id", "123")) // Match attributes for this test
                    .attributeExtractor(
                        (ctx, req) -> Map.of("tenant_id", "123")) // Extracting mock attributes
                    .tokenCostFunction((ctx, req) -> 1) // Token cost per request
                    .rateLimit(
                        RateLimiterConfiguration.RateLimit.builder()
                            .tokens(10) // Bucket size
                            .refreshPeriodSeconds(60) // Bucket refresh period
                            .build())
                    .build()),
            mockRateLimiterFactory);
  }

  @Test
  void testRequestAllowedWhenRateLimitNotExceeded() {
    when(mockRateLimiterFactory.getRateLimiter(any())).thenReturn(mockRateLimiter);
    // Mock rate limiter to allow the request
    when(mockRateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);

    // Intercept the call
    ServerCall.Listener<String> listener =
        rateLimiterInterceptor.interceptCall(mockServerCall, mockMetadata, mockHandler);

    // Ensure listener isn't null
    assertNotNull(listener);

    // Call onMessage to assert normal behavior
    listener.onMessage("testMessage");

    // Verify server call is not closed with error
    verify(mockServerCall, never()).close(any(Status.class), any(Metadata.class));

    // Verify message is passed downstream
    verify(mockListener, times(1)).onMessage("testMessage");
  }

  @Test
  void testRequestRejectedWhenRateLimitExceeded() {
    when(mockRateLimiterFactory.getRateLimiter(any())).thenReturn(mockRateLimiter);
    // Mock rate limiter to reject the request
    when(mockRateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(false);

    // Intercept the call
    ServerCall.Listener<String> listener =
        rateLimiterInterceptor.interceptCall(mockServerCall, mockMetadata, mockHandler);

    // Ensure listener isn't null
    assertNotNull(listener);

    // Call onMessage to trigger the rate limiting logic
    listener.onMessage("testMessage");

    verify(mockServerCall).getMethodDescriptor();

    // Verify server call is closed with RESOURCE_EXHAUSTED error
    verify(mockServerCall, times(1))
        .close(
            argThat(
                status ->
                    status.getCode() == Status.RESOURCE_EXHAUSTED.getCode()
                        && "Rate limit exceeded".equals(status.getDescription())),
            eq(mockMetadata));

    // Verify the mock listener's onMessage is not called
    verify(mockListener, never()).onMessage(any());
  }

  @Test
  void testInterceptorSkipsIfNoMatchingRateLimitConfig() {
    // Mock a different method name that doesn't match our rate limiter configuration
    when(mockServerCall.getMethodDescriptor())
        .thenReturn(
            MethodDescriptor.<String, String>newBuilder()
                .setFullMethodName("org.example.OtherService/OtherMethod") // Different method name
                .setType(MethodDescriptor.MethodType.UNARY)
                .setRequestMarshaller(mock(MethodDescriptor.Marshaller.class))
                .setResponseMarshaller(mock(MethodDescriptor.Marshaller.class))
                .build());

    // Intercept the call
    ServerCall.Listener<String> listener =
        rateLimiterInterceptor.interceptCall(mockServerCall, mockMetadata, mockHandler);

    // Ensure listener isn't null
    assertNotNull(listener);

    // Call onMessage to test behavior for unmatched configurations
    listener.onMessage("testMessage");

    // Verify that the server call is not closed
    verify(mockServerCall, never()).close(any(Status.class), any(Metadata.class));

    // Verify the message is passed downstream
    verify(mockListener, times(1)).onMessage("testMessage");
  }

  @Test
  void testInterceptorHandlesEmptyRateLimiterConfiguration() {
    // Initialize RateLimiterInterceptor with no configurations
    RateLimiterInterceptor emptyConfigInterceptor =
        new RateLimiterInterceptor(List.of(), mockRateLimiterFactory);

    // Intercept the call
    ServerCall.Listener<String> listener =
        emptyConfigInterceptor.interceptCall(mockServerCall, mockMetadata, mockHandler);

    // Ensure listener isn't null
    assertNotNull(listener);

    // Call onMessage to ensure normal behavior
    listener.onMessage("testMessage");

    // Verify server call is not closed with error
    verify(mockServerCall, never()).close(any(Status.class), any(Metadata.class));

    // Verify message is passed downstream
    verify(mockListener, times(1)).onMessage("testMessage");
  }
}
