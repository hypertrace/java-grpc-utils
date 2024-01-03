package org.hypertrace.core.grpcutils.context;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContextualStatusExceptionBuilder {

  @Nonnull private final Status status;
  @Nullable private final Metadata originalTrailers;
  @Nonnull ContextualExceptionDetails details;

  public static ContextualStatusExceptionBuilder from(Status status, RequestContext context) {
    return new ContextualStatusExceptionBuilder(
        status, null, new ContextualExceptionDetails(context));
  }

  public static ContextualStatusExceptionBuilder from(Status status) {
    return new ContextualStatusExceptionBuilder(status, null, new ContextualExceptionDetails());
  }

  public static ContextualStatusExceptionBuilder from(StatusException statusException) {
    return new ContextualStatusExceptionBuilder(
        statusException.getStatus(),
        statusException.getTrailers(),
        new ContextualExceptionDetails());
  }

  public static ContextualStatusExceptionBuilder from(
      StatusRuntimeException statusRuntimeException) {
    return new ContextualStatusExceptionBuilder(
        statusRuntimeException.getStatus(),
        statusRuntimeException.getTrailers(),
        new ContextualExceptionDetails());
  }

  public ContextualStatusExceptionBuilder useStatusDescriptionAsExternalMessage() {
    this.details = this.details.withExternalMessage(this.status.getDescription());
    return this;
  }

  public ContextualStatusExceptionBuilder withExternalMessage(@Nonnull String externalMessage) {
    this.details = this.details.withExternalMessage(externalMessage);
    return this;
  }

  public StatusRuntimeException buildRuntimeException() {
    return status.asRuntimeException(this.collectMetadata());
  }

  public StatusException buildCheckedException() {
    return status.asException(this.collectMetadata());
  }

  private Metadata collectMetadata() {
    Metadata metadataCollector = new Metadata();
    Optional.ofNullable(this.originalTrailers).ifPresent(metadataCollector::merge);
    metadataCollector.merge(this.details.toMetadata());
    return metadataCollector;
  }
}
