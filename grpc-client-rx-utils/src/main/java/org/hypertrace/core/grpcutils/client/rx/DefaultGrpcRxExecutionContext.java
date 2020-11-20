package org.hypertrace.core.grpcutils.client.rx;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.hypertrace.core.grpcutils.context.RequestContext;

class DefaultGrpcRxExecutionContext implements GrpcRxExecutionContext {

  private final RequestContext requestContext;

  DefaultGrpcRxExecutionContext(RequestContext requestContext) {
    this.requestContext = requestContext;
  }

  @Override
  public <TResp> Single<TResp> call(Callable<TResp> callable) {
    return Single.fromCallable(buildContext().wrap(callable));
  }

  @Override
  public <TResp> Single<TResp> wrapSingle(Supplier<Single<TResp>> singleSupplier) {
    try {
      return buildContext().call(singleSupplier::get);
    } catch (Exception e) {
      return Single.error(e);
    }
  }

  @Override
  public <TResp> Maybe<TResp> wrapMaybe(Supplier<Maybe<TResp>> maybeSupplier) {
    try {
      return buildContext().call(maybeSupplier::get);
    } catch (Exception e) {
      return Maybe.error(e);
    }
  }

  @Override
  public Completable run(Runnable runnable) {
    return Completable.fromRunnable(buildContext().wrap(runnable));
  }

  @Override
  public <TResponse> Observable<TResponse> stream(
      Consumer<StreamObserver<TResponse>> streamConsumer) {
    return Observable.create(
        emitter ->
            buildContext()
                .run(() -> streamConsumer.accept(new StreamingClientResponseObserver<>(emitter))));
  }

  private Context buildContext() {
    return Context.current().withValue(RequestContext.CURRENT, this.requestContext);
  }
}
