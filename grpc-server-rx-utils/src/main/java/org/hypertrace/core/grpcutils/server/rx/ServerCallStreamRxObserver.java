package org.hypertrace.core.grpcutils.server.rx;

import io.grpc.stub.ServerCallStreamObserver;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.MaybeObserver;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.observers.DefaultObserver;

public class ServerCallStreamRxObserver<T> extends DefaultObserver<T>
    implements Observer<T>, MaybeObserver<T>, SingleObserver<T>, CompletableObserver {

  private final ServerCallStreamObserver<T> serverCallStreamObserver;

  ServerCallStreamRxObserver(ServerCallStreamObserver<T> serverCallStreamObserver) {
    this.serverCallStreamObserver = serverCallStreamObserver;
  }

  @Override
  protected void onStart() {
    this.serverCallStreamObserver.setOnCancelHandler(this::cancel);
  }

  @Override
  public void onSuccess(@NonNull T value) {
    this.onNext(value);
    this.onComplete();
  }

  @Override
  public void onNext(@NonNull T value) {
    this.serverCallStreamObserver.onNext(value);
  }

  @Override
  public void onError(@NonNull Throwable throwable) {
    this.serverCallStreamObserver.onError(throwable);
  }

  @Override
  public void onComplete() {
    this.serverCallStreamObserver.onCompleted();
  }
}
