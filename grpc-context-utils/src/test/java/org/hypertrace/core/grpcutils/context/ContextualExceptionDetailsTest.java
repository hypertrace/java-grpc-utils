package org.hypertrace.core.grpcutils.context;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static org.hypertrace.core.grpcutils.context.ContextualExceptionDetails.EXTERNAL_MESSAGE_KEY;
import static org.hypertrace.core.grpcutils.context.RequestContextConstants.REQUEST_ID_HEADER_KEY;
import static org.hypertrace.core.grpcutils.context.RequestContextConstants.TENANT_ID_HEADER_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContextualExceptionDetailsTest {
  static final String TEST_REQUEST_ID = "example-request-id";
  static final String TEST_TENANT = "example-tenant";
  static final RequestContext TEST_CONTEXT =
      new RequestContext()
          .put(REQUEST_ID_HEADER_KEY, TEST_REQUEST_ID)
          .put(TENANT_ID_HEADER_KEY, TEST_TENANT);

  @Test
  void convertsExceptionWithoutMessageToMetadata() {
    StatusException exception =
        ContextualStatusExceptionBuilder.from(
                Status.INVALID_ARGUMENT.withDescription("Some internal description"), TEST_CONTEXT)
            .buildCheckedException();

    assertEquals(
        Set.of(REQUEST_ID_HEADER_KEY, TENANT_ID_HEADER_KEY), exception.getTrailers().keys());
    assertEquals(TEST_CONTEXT, RequestContext.fromMetadata(exception.getTrailers()));
  }

  @Test
  void convertsExceptionWithDescriptionAsExternalMessageToMetadata() {
    StatusException exception =
        ContextualStatusExceptionBuilder.from(
                Status.INVALID_ARGUMENT.withDescription("Some description"), TEST_CONTEXT)
            .useStatusDescriptionAsExternalMessage()
            .buildCheckedException();

    assertEquals(
        Set.of(REQUEST_ID_HEADER_KEY, TENANT_ID_HEADER_KEY, EXTERNAL_MESSAGE_KEY.originalName()),
        exception.getTrailers().keys());
    assertEquals(TEST_CONTEXT, RequestContext.fromMetadata(exception.getTrailers()));
    assertEquals("Some description", exception.getTrailers().get(EXTERNAL_MESSAGE_KEY));
  }

  @Test
  void convertsExceptionWithCustomExternalMessageToMetadata() {
    StatusException exception =
        ContextualStatusExceptionBuilder.from(
                Status.INVALID_ARGUMENT.withDescription("Some internal description"), TEST_CONTEXT)
            .withExternalMessage("custom external description")
            .buildCheckedException();

    assertEquals(
        Set.of(REQUEST_ID_HEADER_KEY, TENANT_ID_HEADER_KEY, EXTERNAL_MESSAGE_KEY.originalName()),
        exception.getTrailers().keys());
    assertEquals(TEST_CONTEXT, RequestContext.fromMetadata(exception.getTrailers()));
    assertEquals("custom external description", exception.getTrailers().get(EXTERNAL_MESSAGE_KEY));
  }

  @Test
  void emptyIfUnableToParseContext() {
    assertEquals(
        Optional.empty(),
        ContextualExceptionDetails.fromMetadata(
            metadataFromMap(Map.of("random-key", "random-value"))));
  }

  @Test
  void parsesMetadataWithoutMessage() {
    assertEquals(
        Optional.of(new ContextualExceptionDetails(TEST_CONTEXT)),
        ContextualExceptionDetails.fromMetadata(
            metadataFromMap(
                Map.of(
                    REQUEST_ID_HEADER_KEY, TEST_REQUEST_ID, TENANT_ID_HEADER_KEY, TEST_TENANT))));
  }

  @Test
  void parsesMetadataWithMessage() {
    assertEquals(
        Optional.of(
            new ContextualExceptionDetails(TEST_CONTEXT).withExternalMessage("test message")),
        ContextualExceptionDetails.fromMetadata(
            metadataFromMap(
                Map.of(
                    REQUEST_ID_HEADER_KEY,
                    TEST_REQUEST_ID,
                    TENANT_ID_HEADER_KEY,
                    TEST_TENANT,
                    EXTERNAL_MESSAGE_KEY.originalName(),
                    "test message"))));
  }

  @Test
  void buildsFromExistingException() {
    StatusRuntimeException exception =
        ContextualStatusExceptionBuilder.from(
                Status.INVALID_ARGUMENT
                    .withDescription("test message")
                    .asException(TEST_CONTEXT.buildTrailers()))
            .useStatusDescriptionAsExternalMessage()
            .buildRuntimeException();

    assertEquals(
        Set.of(REQUEST_ID_HEADER_KEY, TENANT_ID_HEADER_KEY, EXTERNAL_MESSAGE_KEY.originalName()),
        exception.getTrailers().keys());
    assertEquals(TEST_CONTEXT, RequestContext.fromMetadata(exception.getTrailers()));
    assertEquals("test message", exception.getTrailers().get(EXTERNAL_MESSAGE_KEY));
  }

  @Test
  void buildsFromExceptionWithCustomTrailers() {
    Key<String> customKey = Key.of("test", ASCII_STRING_MARSHALLER);
    Metadata customMetadata = new Metadata();
    customMetadata.put(customKey, "test-value");
    customMetadata.put(EXTERNAL_MESSAGE_KEY, "should be ignored");
    StatusException exception =
        ContextualStatusExceptionBuilder.from(
                Status.INVALID_ARGUMENT
                    .withDescription("test message")
                    .asRuntimeException(customMetadata))
            .withExternalMessage("custom message")
            .buildCheckedException();

    assertEquals(
        Set.of(customKey.originalName(), EXTERNAL_MESSAGE_KEY.originalName()),
        exception.getTrailers().keys());
    assertEquals("custom message", exception.getTrailers().get(EXTERNAL_MESSAGE_KEY));
    assertEquals("test-value", exception.getTrailers().get(customKey));
  }

  Metadata metadataFromMap(Map<String, String> metadataMap) {
    Metadata metadata = new Metadata();
    metadataMap.forEach((key, value) -> metadata.put(Key.of(key, ASCII_STRING_MARSHALLER), value));
    return metadata;
  }
}
