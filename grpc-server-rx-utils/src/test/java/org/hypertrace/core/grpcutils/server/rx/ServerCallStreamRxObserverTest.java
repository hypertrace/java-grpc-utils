package org.hypertrace.core.grpcutils.server.rx;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.grpc.stub.ServerCallStreamObserver;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServerCallStreamRxObserverTest {

  @Mock ServerCallStreamObserver<Object> mockGrpcObserver;

  @Test
  void propagatesObservableValues() {
    Observable.just("foo", "bar")
        .blockingSubscribe(new ServerCallStreamRxObserver<>(this.mockGrpcObserver));
    verifyValuesAndCompletion("foo", "bar");
  }

  @Test
  void propagatesEmptyObservable() {
    Observable.empty().blockingSubscribe(new ServerCallStreamRxObserver<>(this.mockGrpcObserver));
    verifyValuesAndCompletion();
  }

  @Test
  void propagatesSingleValue() {
    Single.just("single")
        .blockingSubscribe(new ServerCallStreamRxObserver<>(this.mockGrpcObserver));
    verifyValuesAndCompletion("single");
  }

  @Test
  void propagatesMaybeValue() {
    Maybe.just("maybe").blockingSubscribe(new ServerCallStreamRxObserver<>(this.mockGrpcObserver));
    verifyValuesAndCompletion("maybe");
  }

  @Test
  void propagatesEmptyMaybe() {
    Maybe.empty().blockingSubscribe(new ServerCallStreamRxObserver<>(this.mockGrpcObserver));
    verifyValuesAndCompletion();
  }

  @Test
  void propagatesCompletableCompletion() {
    Completable.complete()
        .blockingSubscribe(new ServerCallStreamRxObserver<>(this.mockGrpcObserver));
    verifyValuesAndCompletion();
  }

  @Test
  void propagatesObservableError() {
    Throwable error = new IllegalArgumentException("observable");
    Observable.error(error)
        .blockingSubscribe(new ServerCallStreamRxObserver<>(this.mockGrpcObserver));
    verifyThrows(error);
  }

  @Test
  void propagateSingleError() {
    Throwable error = new IllegalArgumentException("single");
    Single.error(error).blockingSubscribe(new ServerCallStreamRxObserver<>(this.mockGrpcObserver));
    verifyThrows(error);
  }

  @Test
  void propagatesMaybeError() {
    Throwable error = new IllegalArgumentException("maybe");
    Maybe.error(error).blockingSubscribe(new ServerCallStreamRxObserver<>(this.mockGrpcObserver));
    verifyThrows(error);
  }

  @Test
  void propagatesCompletableError() {
    Throwable error = new IllegalArgumentException("completable");
    Completable.error(error)
        .blockingSubscribe(new ServerCallStreamRxObserver<>(this.mockGrpcObserver));
    verifyThrows(error);
  }

  @Test
  void propagatesCancellationRequest() {
    Observable.just("first", "second")
        .doAfterNext(
            value -> {
              // Capture the cancellation handler and invoke it to prevent values beyond the first
              ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
              verify(this.mockGrpcObserver).setOnCancelHandler(captor.capture());
              captor.getValue().run();
            })
        .subscribe(new ServerCallStreamRxObserver<>(this.mockGrpcObserver));

    verify(this.mockGrpcObserver).onNext("first");
    verifyNoMoreInteractions(this.mockGrpcObserver);
  }

  private void verifyValuesAndCompletion(String... values) {
    InOrder callOrder = inOrder(this.mockGrpcObserver);

    callOrder.verify(this.mockGrpcObserver).setOnCancelHandler(any());
    Arrays.asList(values).forEach(value -> callOrder.verify(this.mockGrpcObserver).onNext(value));
    callOrder.verify(this.mockGrpcObserver).onCompleted();

    verifyNoMoreInteractions(this.mockGrpcObserver);
  }

  private void verifyThrows(Throwable throwable) {
    InOrder callOrder = inOrder(this.mockGrpcObserver);

    callOrder.verify(this.mockGrpcObserver).setOnCancelHandler(any());
    callOrder.verify(this.mockGrpcObserver).onError(throwable);

    verifyNoMoreInteractions(this.mockGrpcObserver);
  }
}
