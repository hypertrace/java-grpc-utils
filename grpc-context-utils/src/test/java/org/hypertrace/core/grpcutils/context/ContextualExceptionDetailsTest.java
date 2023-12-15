package org.hypertrace.core.grpcutils.context;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static org.hypertrace.core.grpcutils.context.ContextualExceptionDetails.EXTERNAL_MESSAGE_KEY;
import static org.hypertrace.core.grpcutils.context.RequestContextConstants.REQUEST_ID_HEADER_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.Status;
import io.grpc.StatusException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ContextualExceptionDetailsTest {
  static final String TEST_REQUEST_ID = "example-request-id";
  static final RequestContext TEXT_CONTEXT =
      new RequestContext().put(REQUEST_ID_HEADER_KEY, TEST_REQUEST_ID);

  @Test
  void convertsExceptionWithoutMessageToMetadata() {
    StatusException exception =
        ContextualStatusExceptionBuilder.from(
                Status.INVALID_ARGUMENT.withDescription("Some internal description"), TEXT_CONTEXT)
            .buildCheckedException();

    assertEquals(
        Map.of(REQUEST_ID_HEADER_KEY, TEST_REQUEST_ID), metadataAsMap(exception.getTrailers()));
  }

  @Test
  void convertsExceptionWithDescriptionAsExternalMessageToMetadata() {
    StatusException exception =
        ContextualStatusExceptionBuilder.from(
                Status.INVALID_ARGUMENT.withDescription("Some description"), TEXT_CONTEXT)
            .useStatusDescriptionAsExternalMessage()
            .buildCheckedException();

    assertEquals(
        Map.of(
            REQUEST_ID_HEADER_KEY,
            TEST_REQUEST_ID,
            EXTERNAL_MESSAGE_KEY.originalName(),
            "Some description"),
        metadataAsMap(exception.getTrailers()));
  }

  @Test
  void convertsExceptionWithCustomExternalMessageToMetadata() {
    StatusException exception =
        ContextualStatusExceptionBuilder.from(
                Status.INVALID_ARGUMENT.withDescription("Some internal description"), TEXT_CONTEXT)
            .withExternalMessage("custom external description")
            .buildCheckedException();

    assertEquals(
        Map.of(
            REQUEST_ID_HEADER_KEY,
            TEST_REQUEST_ID,
            EXTERNAL_MESSAGE_KEY.originalName(),
            "custom external description"),
        metadataAsMap(exception.getTrailers()));
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
        Optional.of(new ContextualExceptionDetails(TEXT_CONTEXT)),
        ContextualExceptionDetails.fromMetadata(
            metadataFromMap(Map.of(REQUEST_ID_HEADER_KEY, TEST_REQUEST_ID))));
  }

  @Test
  void parsesMetadataWithMessage() {
    assertEquals(
        Optional.of(new ContextualExceptionDetails(TEXT_CONTEXT, "test message")),
        ContextualExceptionDetails.fromMetadata(
            metadataFromMap(
                Map.of(
                    REQUEST_ID_HEADER_KEY,
                    TEST_REQUEST_ID,
                    EXTERNAL_MESSAGE_KEY.originalName(),
                    "test message"))));
  }

  Map<String, String> metadataAsMap(Metadata metadata) {
    return metadata.keys().stream()
        .collect(
            Collectors.toUnmodifiableMap(
                Function.identity(), key -> metadata.get(Key.of(key, ASCII_STRING_MARSHALLER))));
  }

  Metadata metadataFromMap(Map<String, String> metadataMap) {
    Metadata metadata = new Metadata();
    metadataMap.forEach((key, value) -> metadata.put(Key.of(key, ASCII_STRING_MARSHALLER), value));
    return metadata;
  }
}
