package org.hypertrace.core.grpcutils.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class DefaultContextualKeyTest {

  @Test
  void callsProvidedMethodsInContext() {
    RequestContext testContext = RequestContext.forTenantId("test-tenant");
    ContextualKey<String> key = new DefaultContextualKey<>(testContext, "input");

    Function<String, String> testFunction =
        value ->
            "returned: "
                + value
                + " for "
                + RequestContext.CURRENT.get().getTenantId().orElseThrow();

    assertEquals("returned: input for test-tenant", key.callInContext(testFunction));

    Supplier<String> testSupplier =
        () -> "returned for " + RequestContext.CURRENT.get().getTenantId().orElseThrow();

    assertEquals("returned for test-tenant", key.callInContext(testSupplier));
  }

  @Test
  void runsProvidedMethodInContext() {
    RequestContext testContext = RequestContext.forTenantId("test-tenant");
    ContextualKey<String> key = new DefaultContextualKey<>(testContext, "input");

    Consumer<String> testConsumer = mock(Consumer.class);

    doAnswer(
            invocation -> {
              assertSame(testContext, RequestContext.CURRENT.get());
              return null;
            })
        .when(testConsumer)
        .accept(any());
    key.runInContext(testConsumer);
    verify(testConsumer, times(1)).accept(eq("input"));

    Runnable testRunnable = mock(Runnable.class);
    key.runInContext(testRunnable);
    verify(testRunnable, times(1)).run();
  }

  @Test
  void matchesEquivalentKeysOnly() {
    RequestContext tenant1Context = RequestContext.forTenantId("first");
    RequestContext alternateTenant1Context = RequestContext.forTenantId("first");
    alternateTenant1Context.add("other", "value");
    RequestContext tenant2Context = RequestContext.forTenantId("second");

    assertEquals(
        new DefaultContextualKey<>(tenant1Context, "input"),
        new DefaultContextualKey<>(tenant1Context, "input"));

    assertEquals(
        new DefaultContextualKey<>(tenant1Context, "input"),
        new DefaultContextualKey<>(alternateTenant1Context, "input"));

    assertNotEquals(
        new DefaultContextualKey<>(tenant1Context, "input"),
        new DefaultContextualKey<>(tenant2Context, "input"));

    assertNotEquals(
        new DefaultContextualKey<>(tenant1Context, "input"),
        new DefaultContextualKey<>(tenant1Context, "other input"));
  }
}
