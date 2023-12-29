package org.hypertrace.core.grpcutils.server;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static org.hypertrace.core.grpcutils.context.RequestContextConstants.REQUEST_ID_HEADER_KEY;
import static org.hypertrace.core.grpcutils.context.RequestContextConstants.TENANT_ID_HEADER_KEY;
import static org.hypertrace.core.grpcutils.context.RequestContextConstants.TENANT_ID_METADATA_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusException;
import java.util.Optional;
import java.util.Set;
import org.hypertrace.core.grpcutils.context.ContextualStatusExceptionBuilder;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThrowableResponseInterceptorTest {
  @Mock ServerCallHandler<String, String> mockHandler;
  @Mock ServerCall<String, String> mockCall;
  @Mock Metadata mockMetadata;
  @Captor ArgumentCaptor<ServerCall<String, String>> callArgumentCaptor;
  @Captor ArgumentCaptor<Metadata> metadataArgumentCaptor;
  @Captor ArgumentCaptor<Status> statusArgumentCaptor;

  ThrowableResponseInterceptor interceptor = new ThrowableResponseInterceptor();

  @Test
  void propagatesCallToNextHandler() {
    interceptor.interceptCall(mockCall, mockMetadata, mockHandler);

    verify(mockHandler).startCall(callArgumentCaptor.capture(), any(Metadata.class));
    callArgumentCaptor.getValue().sendMessage("example-message");
    verify(mockCall).sendMessage("example-message");

    callArgumentCaptor.getValue().close(Status.OK, mockMetadata);
    verify(mockCall).close(eq(Status.OK), any(Metadata.class));
  }

  @Test
  void mergesTrailersForUnknownStatus() {
    RequestContext callingContext = RequestContext.forTenantId("test");
    interceptor.interceptCall(mockCall, callingContext.buildTrailers(), mockHandler);

    verify(mockHandler).startCall(callArgumentCaptor.capture(), any(Metadata.class));

    callArgumentCaptor.getValue().close(Status.UNKNOWN, new Metadata());
    verify(mockCall).close(eq(Status.UNKNOWN), metadataArgumentCaptor.capture());
    Metadata sentMetadata = metadataArgumentCaptor.getValue();
    assertEquals(Set.of(REQUEST_ID_HEADER_KEY, TENANT_ID_HEADER_KEY), sentMetadata.keys());

    assertEquals("test", sentMetadata.get(TENANT_ID_METADATA_KEY));
    assertEquals(
        callingContext.getRequestId(),
        Optional.ofNullable(
            sentMetadata.get(Key.of(REQUEST_ID_HEADER_KEY, ASCII_STRING_MARSHALLER))));
  }

  @Test
  void mergesTrailersForGrpcException() {
    RequestContext callingContext = RequestContext.forTenantId("test");
    interceptor.interceptCall(mockCall, callingContext.buildTrailers(), mockHandler);

    verify(mockHandler).startCall(callArgumentCaptor.capture(), any(Metadata.class));

    StatusException exception =
        ContextualStatusExceptionBuilder.from(Status.INVALID_ARGUMENT)
            .withExternalMessage("external message value")
            .buildCheckedException();
    callArgumentCaptor.getValue().close(exception.getStatus(), exception.getTrailers());
    verify(mockCall).close(eq(exception.getStatus()), metadataArgumentCaptor.capture());
    Metadata sentMetadata = metadataArgumentCaptor.getValue();

    assertEquals(
        Set.of(REQUEST_ID_HEADER_KEY, TENANT_ID_HEADER_KEY, "external-message"),
        sentMetadata.keys());
    assertEquals("test", sentMetadata.get(TENANT_ID_METADATA_KEY));
    assertEquals(
        callingContext.getRequestId(),
        Optional.ofNullable(
            sentMetadata.get(Key.of(REQUEST_ID_HEADER_KEY, ASCII_STRING_MARSHALLER))));
    assertEquals(
        "external message value",
        sentMetadata.get(Key.of("external-message", ASCII_STRING_MARSHALLER)));
  }

  @Test
  void mergesTrailersForNestedException() {

    RequestContext callingContext = RequestContext.forTenantId("test");
    interceptor.interceptCall(mockCall, callingContext.buildTrailers(), mockHandler);

    verify(mockHandler).startCall(callArgumentCaptor.capture(), any(Metadata.class));

    Status status =
        Status.UNKNOWN.withCause(
            ContextualStatusExceptionBuilder.from(
                    Status.INVALID_ARGUMENT.withDescription("invalid arg"))
                .useStatusDescriptionAsExternalMessage()
                .buildCheckedException());
    callArgumentCaptor.getValue().close(status, new Metadata());
    verify(mockCall).close(statusArgumentCaptor.capture(), metadataArgumentCaptor.capture());
    Metadata sentMetadata = metadataArgumentCaptor.getValue();
    Status sentStatus = statusArgumentCaptor.getValue();

    assertEquals(Code.INTERNAL, sentStatus.getCode());
    assertEquals(status.getCause().getMessage(), sentStatus.getDescription());
    assertEquals(status.getCause(), sentStatus.getCause());

    assertEquals(
        Set.of(REQUEST_ID_HEADER_KEY, TENANT_ID_HEADER_KEY, "external-message"),
        sentMetadata.keys());
    assertEquals("test", sentMetadata.get(TENANT_ID_METADATA_KEY));
    assertEquals(
        callingContext.getRequestId(),
        Optional.ofNullable(
            sentMetadata.get(Key.of(REQUEST_ID_HEADER_KEY, ASCII_STRING_MARSHALLER))));
    assertEquals(
        "invalid arg", sentMetadata.get(Key.of("external-message", ASCII_STRING_MARSHALLER)));
  }
}
