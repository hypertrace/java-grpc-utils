package org.hypertrace.core.grpcutils.client.rx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.grpc.Context;
import java.util.Optional;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GrpcRxExecutionContextTest {

  @Mock RequestContext firstMockContext;

  @Mock RequestContext secondMockContext;

  @Test
  void canCreateExecutionContextWithCurrentContext() throws Exception {
    assertSame(
        this.firstMockContext,
        Context.current()
            .withValue(RequestContext.CURRENT, this.firstMockContext)
            .call(
                () -> GrpcRxExecutionContext.forCurrentContext().call(RequestContext.CURRENT::get))
            .blockingGet());
  }

  @Test
  void canCreateExecutionContextWithProvidedContext() throws Exception {
    assertSame(
        this.secondMockContext,
        Context.current()
            .withValue(RequestContext.CURRENT, this.firstMockContext)
            .call(
                () ->
                    GrpcRxExecutionContext.forContext(this.secondMockContext)
                        .call(RequestContext.CURRENT::get))
            .blockingGet());
  }

  @Test
  void canCreateExecutionContextForProvidedTenant() throws Exception {
    final String testTenant = "testTenant";
    assertEquals(
        Optional.of(testTenant),
        GrpcRxExecutionContext.forTenantContext(testTenant)
            .call(RequestContext.CURRENT::get)
            .map(RequestContext::getTenantId)
            .blockingGet());
  }
}
