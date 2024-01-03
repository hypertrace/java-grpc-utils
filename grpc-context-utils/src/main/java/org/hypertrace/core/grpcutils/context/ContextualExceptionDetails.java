package org.hypertrace.core.grpcutils.context;

import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@ToString
@EqualsAndHashCode
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContextualExceptionDetails {
  static final Key<String> EXTERNAL_MESSAGE_KEY =
      Key.of("external-message", Metadata.ASCII_STRING_MARSHALLER);
  @Nullable RequestContext requestContext;
  @Nullable String externalMessage;

  public Optional<String> getExternalMessage() {
    return Optional.ofNullable(this.externalMessage);
  }

  public Optional<RequestContext> getRequestContext() {
    return Optional.ofNullable(this.requestContext);
  }

  ContextualExceptionDetails(RequestContext requestContext) {
    this(requestContext, null);
  }

  ContextualExceptionDetails() {
    this(null);
  }

  Metadata toMetadata() {
    Metadata metadata = new Metadata();
    this.getExternalMessage().ifPresent(value -> metadata.put(EXTERNAL_MESSAGE_KEY, value));
    this.getRequestContext().map(RequestContext::buildTrailers).ifPresent(metadata::merge);
    return metadata;
  }

  ContextualExceptionDetails withExternalMessage(@Nullable String externalMessage) {
    return new ContextualExceptionDetails(this.getRequestContext().orElse(null), externalMessage);
  }

  public static Optional<ContextualExceptionDetails> fromMetadata(Metadata metadata) {
    RequestContext requestContext = RequestContext.fromMetadata(metadata);
    if (requestContext.getRequestId().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        new ContextualExceptionDetails(requestContext, metadata.get(EXTERNAL_MESSAGE_KEY)));
  }
}
