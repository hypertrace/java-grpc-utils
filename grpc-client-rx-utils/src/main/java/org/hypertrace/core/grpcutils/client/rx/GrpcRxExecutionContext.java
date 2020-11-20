package org.hypertrace.core.grpcutils.client.rx;

import io.grpc.stub.StreamObserver;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.hypertrace.core.grpcutils.context.RequestContext;

/** An execution context that can turn various types of executions into their Rx equivalents. */
public interface GrpcRxExecutionContext {
  /**
   * Executes the given callable in this execution context, returning any result or error as a
   * {@link Single}.
   */
  <TResp> Single<TResp> call(Callable<TResp> callable);

  /**
   * Creates the provided single in this execution context, returning the result.
   */
  <TResp> Single<TResp> wrapSingle(Supplier<Single<TResp>> singleSupplier);

  /**
   * Creates the provided maybe in this execution context, returning the result.
   */
  <TResp> Maybe<TResp> wrapMaybe(Supplier<Maybe<TResp>> maybeSupplier);

  /**
   * Executes the given runnable in this execution context, triggering completion or error once the
   * call completes.
   */
  Completable run(Runnable runnable);

  /**
   * Provides a stream observer to the provided consumer, converting the result into an Observable.
   */
  <TResponse> Observable<TResponse> stream(Consumer<StreamObserver<TResponse>> requestExecutor);

  static GrpcRxExecutionContext forCurrentContext() {
    return forContext(RequestContext.CURRENT.get());
  }

  static GrpcRxExecutionContext forContext(RequestContext requestContext) {
    return new DefaultGrpcRxExecutionContext(requestContext);
  }

  static GrpcRxExecutionContext forTenantContext(String tenantId) {
    return forContext(RequestContext.forTenantId(tenantId));
  }
}
