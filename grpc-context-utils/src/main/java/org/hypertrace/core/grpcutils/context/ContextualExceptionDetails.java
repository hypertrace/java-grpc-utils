package org.hypertrace.core.grpcutils.context;

import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class ContextualExceptionDetails {
  static final Key<String> EXTERNAL_MESSAGE_KEY =
      Key.of("external-message", Metadata.ASCII_STRING_MARSHALLER);
  @Nonnull RequestContext requestContext;
  @Nullable String externalMessage;

  Optional<String> getExternalMessage() {
    return Optional.ofNullable(this.externalMessage);
  }

  ContextualExceptionDetails(@Nonnull RequestContext requestContext) {
    this(requestContext, null);
  }

  Metadata toMetadata() {
    Metadata metadata = new Metadata();
    this.getExternalMessage().ifPresent(value -> metadata.put(EXTERNAL_MESSAGE_KEY, value));
    metadata.merge(this.getRequestContext().buildTrailers());
    return metadata;
  }

  ContextualExceptionDetails withExternalMessage(@Nullable String externalMessage) {
    return new ContextualExceptionDetails(this.getRequestContext(), externalMessage);
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
