package org.hypertrace.circuitbreaker.grpcutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ServiceCircuitBreakerInterceptorTest {

  private static final String INSIGHTS_SERVICE =
      "ai.traceable.platform.insights.api.v1.InsightsService";
  private static final String OTHER_SERVICE = "ai.traceable.platform.other.OtherService";
  private static final String OPEN_QUICKLY =
      "slowCallDurationThreshold = 1h\n"
          + "minimumNumberOfCalls = 1\n"
          + "failureRateThreshold = 1\n"
          + "slidingWindowSize = 1\n"
          + "slidingWindowType = COUNT_BASED\n"
          + "waitDurationInOpenState = 60s\n";

  @Test
  void absentConfigCallsTheDelegate() {
    Channel channel = mock(Channel.class);
    ClientInterceptor interceptor =
        CircuitBreakerInterceptorFactory.forService(ConfigFactory.empty());

    complete(interceptor, channel, INSIGHTS_SERVICE, Status.UNAVAILABLE);
    complete(interceptor, channel, INSIGHTS_SERVICE, Status.UNAVAILABLE);
    verify(channel, times(2)).newCall(any(), any());
  }

  @Test
  void missingRootEnabledCallsTheDelegate() {
    Channel channel = mock(Channel.class);
    ClientInterceptor interceptor =
        CircuitBreakerInterceptorFactory.forService(
            circuitBreaker(serviceBlock(INSIGHTS_SERVICE, OPEN_QUICKLY)));

    complete(interceptor, channel, INSIGHTS_SERVICE, Status.UNAVAILABLE);
    complete(interceptor, channel, INSIGHTS_SERVICE, Status.UNAVAILABLE);
    verify(channel, times(2)).newCall(any(), any());
  }

  @Test
  void rootEnabledFalseCallsTheDelegate() {
    Channel channel = mock(Channel.class);
    ClientInterceptor interceptor =
        CircuitBreakerInterceptorFactory.forService(
            circuitBreaker("enabled = false\n" + serviceBlock(INSIGHTS_SERVICE, OPEN_QUICKLY)));

    complete(interceptor, channel, INSIGHTS_SERVICE, Status.UNAVAILABLE);
    complete(interceptor, channel, INSIGHTS_SERVICE, Status.UNAVAILABLE);
    verify(channel, times(2)).newCall(any(), any());
  }

  @Test
  void enabledServiceCallsTheDelegate() {
    Channel channel = mock(Channel.class);
    ClientInterceptor interceptor =
        CircuitBreakerInterceptorFactory.forService(
            circuitBreaker(
                "enabled = true\n"
                    + "defaultThresholds { slowCallDurationThreshold = 1h }\n"
                    + serviceBlock(INSIGHTS_SERVICE, "")));

    complete(interceptor, channel, INSIGHTS_SERVICE, Status.OK);
    verify(channel).newCall(any(), any());
  }

  @Test
  void serviceFailuresDoNotTripAnotherService() {
    Channel channel = mock(Channel.class);
    ClientInterceptor interceptor =
        CircuitBreakerInterceptorFactory.forService(
            circuitBreaker(
                "enabled = true\n"
                    + serviceBlock(INSIGHTS_SERVICE, OPEN_QUICKLY)
                    + serviceBlock(OTHER_SERVICE, OPEN_QUICKLY)));

    complete(interceptor, channel, INSIGHTS_SERVICE, Status.UNAVAILABLE);
    complete(interceptor, channel, OTHER_SERVICE, Status.OK);
    verify(channel, times(2)).newCall(any(), any());

    StatusRuntimeException rejected = reject(interceptor, channel, INSIGHTS_SERVICE);
    assertEquals(Status.Code.RESOURCE_EXHAUSTED, rejected.getStatus().getCode());
    verify(channel, times(2)).newCall(any(), any());
  }

  @Test
  void unavailableOpensTheBreakerWithoutCallingTheDelegate() {
    Channel channel = mock(Channel.class);
    ClientInterceptor interceptor =
        CircuitBreakerInterceptorFactory.forService(
            circuitBreaker("enabled = true\n" + serviceBlock(INSIGHTS_SERVICE, OPEN_QUICKLY)));

    complete(interceptor, channel, INSIGHTS_SERVICE, Status.UNAVAILABLE);

    StatusRuntimeException rejected = reject(interceptor, channel, INSIGHTS_SERVICE);
    assertEquals(Status.Code.RESOURCE_EXHAUSTED, rejected.getStatus().getCode());
    assertNotNull(rejected.getStatus().getDescription());
    assertFalse(rejected.getStatus().getDescription().isEmpty());
    verify(channel, times(1)).newCall(any(), any());
  }

  @Test
  void internalDoesNotOpenTheBreakerForTheDefaultFailureStatuses() {
    Channel channel = mock(Channel.class);
    ClientInterceptor interceptor =
        CircuitBreakerInterceptorFactory.forService(
            circuitBreaker("enabled = true\n" + serviceBlock(INSIGHTS_SERVICE, OPEN_QUICKLY)));

    complete(interceptor, channel, INSIGHTS_SERVICE, Status.INTERNAL);
    complete(interceptor, channel, INSIGHTS_SERVICE, Status.OK);
    verify(channel, times(2)).newCall(any(), any());
  }

  @Test
  void onlyConfiguredUnavailableCountsAsFailure() {
    Channel channel = mock(Channel.class);
    ClientInterceptor interceptor =
        CircuitBreakerInterceptorFactory.forService(
            circuitBreaker(
                "enabled = true\n"
                    + "defaultThresholds { failureStatusCodes = [UNAVAILABLE] }\n"
                    + serviceBlock(INSIGHTS_SERVICE, OPEN_QUICKLY)));

    complete(interceptor, channel, INSIGHTS_SERVICE, Status.INTERNAL);
    complete(interceptor, channel, INSIGHTS_SERVICE, Status.DEADLINE_EXCEEDED);
    complete(interceptor, channel, INSIGHTS_SERVICE, Status.UNAVAILABLE);

    StatusRuntimeException rejected = reject(interceptor, channel, INSIGHTS_SERVICE);
    assertEquals(Status.Code.RESOURCE_EXHAUSTED, rejected.getStatus().getCode());
    verify(channel, times(3)).newCall(any(), any());
  }

  @Test
  void unknownFailureStatusFailsConstruction() {
    Config config =
        circuitBreaker(
            "enabled = true\n"
                + serviceBlock(INSIGHTS_SERVICE, "failureStatusCodes = [NOT_A_STATUS]"));

    assertThrows(
        ConfigException.BadValue.class, () -> CircuitBreakerInterceptorFactory.forService(config));
  }

  @Test
  void disabledServicePassesThrough() {
    Channel channel = mock(Channel.class);
    ClientInterceptor interceptor =
        CircuitBreakerInterceptorFactory.forService(
            circuitBreaker(
                "enabled = true\n"
                    + serviceBlock(INSIGHTS_SERVICE, "enabled = false\n" + OPEN_QUICKLY)));

    complete(interceptor, channel, INSIGHTS_SERVICE, Status.UNAVAILABLE);
    complete(interceptor, channel, INSIGHTS_SERVICE, Status.UNAVAILABLE);
    verify(channel, times(2)).newCall(any(), any());
  }

  private static Config circuitBreaker(String body) {
    return ConfigFactory.parseString("circuit.breaker.config {\n" + body + "\n}");
  }

  private static String serviceBlock(String serviceName, String body) {
    return "\"" + serviceName + "\" {\n" + body + "\n}\n";
  }

  @SuppressWarnings("unchecked")
  private static void complete(
      ClientInterceptor interceptor, Channel channel, String serviceName, Status status) {
    ClientCall<Object, Object> delegate = mock(ClientCall.class);
    when(channel.newCall(any(), any())).thenReturn(delegate);
    ClientCall.Listener<Object> listener = mock(ClientCall.Listener.class);
    Metadata headers = new Metadata();
    ClientCall<Object, Object> call =
        interceptor.interceptCall(method(serviceName), CallOptions.DEFAULT, channel);
    call.start(listener, headers);

    ArgumentCaptor<ClientCall.Listener<Object>> captor =
        ArgumentCaptor.forClass(ClientCall.Listener.class);
    verify(delegate).start(captor.capture(), any());
    Metadata trailers = new Metadata();
    captor.getValue().onClose(status, trailers);
    verify(listener).onClose(status, trailers);
  }

  private static StatusRuntimeException reject(
      ClientInterceptor interceptor, Channel channel, String serviceName) {
    return assertThrows(
        StatusRuntimeException.class,
        () -> interceptor.interceptCall(method(serviceName), CallOptions.DEFAULT, channel));
  }

  @SuppressWarnings("unchecked")
  private static MethodDescriptor<Object, Object> method(String serviceName) {
    MethodDescriptor<Object, Object> method = mock(MethodDescriptor.class);
    when(method.getServiceName()).thenReturn(serviceName);
    return method;
  }
}
