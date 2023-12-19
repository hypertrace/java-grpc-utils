package org.hypertrace.core.grpcutils.context;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import javax.annotation.Nonnull;
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

  private final Status status;
  ContextualExceptionDetails details;

  public static ContextualStatusExceptionBuilder from(Status status, RequestContext context) {
    return new ContextualStatusExceptionBuilder(status, new ContextualExceptionDetails(context));
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
    return status.asRuntimeException(this.details.toMetadata());
  }

  public StatusException buildCheckedException() {
    return status.asException(this.details.toMetadata());
  }
}
