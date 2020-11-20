package org.hypertrace.core.grpcutils.client.rx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.List;
import java.util.Optional;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultGrpcRxExecutionContextTest {

  private static final Optional<String> TEST_TENANT_ID_OPTIONAL = Optional.of("test-tenant-id");
  @Mock RequestContext mockContext;

  @BeforeEach
  void beforeEach() {
    when(mockContext.getTenantId()).thenReturn(TEST_TENANT_ID_OPTIONAL);
  }

  @Test
  void canRunInContext() {
    Completable completable =
        new DefaultGrpcRxExecutionContext(this.mockContext)
            .run(() -> RequestContext.CURRENT.get().getTenantId());
    verifyNoInteractions(this.mockContext);
    completable.subscribe();
    verify(this.mockContext, times(1)).getTenantId();
  }

  @Test
  void canCallInContext() {
    Single<?> single =
        new DefaultGrpcRxExecutionContext(this.mockContext)
            .call(() -> RequestContext.CURRENT.get().getTenantId());
    verifyNoInteractions(this.mockContext);
    assertEquals(TEST_TENANT_ID_OPTIONAL, single.blockingGet());
  }

  @Test
  void canStreamInContext() {
    Observable<Optional<String>> observable =
        new DefaultGrpcRxExecutionContext(this.mockContext)
            .stream(
                observer -> {
                  observer.onNext(RequestContext.CURRENT.get().getTenantId());
                  observer.onCompleted();
                });
    verifyNoInteractions(this.mockContext);
    assertIterableEquals(List.of(TEST_TENANT_ID_OPTIONAL), observable.blockingIterable());
  }

  @Test
  void canPropagateErrors() {
    Completable completable =
        new DefaultGrpcRxExecutionContext(this.mockContext)
            .run(
                () -> {
                  RequestContext.CURRENT.get().getTenantId();
                  throw new UnsupportedOperationException();
                });
    TestObserver<?> testObserver = new TestObserver<>();
    completable.subscribe(testObserver);
    testObserver.assertError(UnsupportedOperationException.class);
  }

  @Test
  void canWrapSingle() {
    Single<?> single =
        new DefaultGrpcRxExecutionContext(this.mockContext)
            .wrapSingle(() -> Single.just(RequestContext.CURRENT.get().getTenantId()));

    assertEquals(TEST_TENANT_ID_OPTIONAL, single.blockingGet());
  }

  @Test
  void canWrapMaybe() {
    Maybe<?> maybe =
        new DefaultGrpcRxExecutionContext(this.mockContext)
            .wrapMaybe(() -> Maybe.just(RequestContext.CURRENT.get().getTenantId()));

    assertEquals(TEST_TENANT_ID_OPTIONAL, maybe.blockingGet());
  }
}
