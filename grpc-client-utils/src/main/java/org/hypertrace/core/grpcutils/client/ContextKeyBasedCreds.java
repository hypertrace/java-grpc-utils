package org.hypertrace.core.grpcutils.client;

import io.grpc.Context;
import java.util.concurrent.Executor;
import org.hypertrace.core.grpcutils.context.RequestContext;

public class ContextKeyBasedCreds extends RequestContextAsCreds {

  private final Context.Key<RequestContext> contextKey;

  public ContextKeyBasedCreds(Context.Key<RequestContext> contextKey) {
    this.contextKey = contextKey;
  }

  @Override
  public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor,
                                   MetadataApplier applier) {
    try {
      applyRequestContext(applier, contextKey.get());
    } catch (RuntimeException e) {
      applyFailure(applier, e);
    }
  }
}
