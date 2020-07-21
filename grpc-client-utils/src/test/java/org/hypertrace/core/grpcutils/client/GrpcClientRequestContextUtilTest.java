package org.hypertrace.core.grpcutils.client;

import io.grpc.Context;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.hypertrace.core.grpcutils.context.RequestContextConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GrpcClientRequestContextUtilTest {
  private static final String TENANT_ID = "test-tenant-id";

  @Test
  public void testExecuteWithTenantHeaderContext() {
    Map<String, String> requestHeaders =
        Map.of("a1", "v1", RequestContextConstants.TENANT_ID_HEADER_KEY, TENANT_ID, "a2", "v2");

    Assertions.assertNull(RequestContext.CURRENT.get());
    Assertions.assertEquals(Optional.of(TENANT_ID),
        GrpcClientRequestContextUtil.executeWithHeadersContext(requestHeaders,
            () -> RequestContext.CURRENT.get().getTenantId()));
  }

  @Test
  public void testExecuteWithHeadersContextNoTenantIdNoAuthHeader_shouldPropagateAllHeaders() {
    Context ctx = Context.current();
    Context previous = ctx.attach();
    try {
      Map<String, String> requestHeaders = Map.of("a1", "v1", "a2", "v2");

      GrpcClientRequestContextUtil.executeWithHeadersContext(requestHeaders, () -> {
        RequestContext requestContext = RequestContext.CURRENT.get();
        Assertions.assertEquals(Map.of("a1", "v1", "a2", "v2"), requestContext.getRequestHeaders());
        return new Object();
      });
    } finally {
      ctx.detach(previous);
    }
  }

  @Test
  public void testExecuteWithHeadersContextJustAuthHeader_shouldPropagateAllHeaders() {
    Context ctx = Context.current();
    Context previous = ctx.attach();
    try {
      Map<String, String> requestHeaders = Map.of("authorization", "v1", "a2", "v2");

      GrpcClientRequestContextUtil.executeWithHeadersContext(requestHeaders, () -> {
        RequestContext requestContext = RequestContext.CURRENT.get();
        Assertions.assertEquals(Map.of("authorization", "v1", "a2", "v2"), requestContext.getRequestHeaders());
        return new Object();
      });
    } finally {
      ctx.detach(previous);
    }
  }

  @Test
  public void testExecuteWithHeadersContextAuthAndTracing_shouldPropagateAllHeaders() {
    Context ctx = Context.current();
    Context previous = ctx.attach();
    try {
      Map<String, String> requestHeaders = Map.of("authorization", "v1", "a2", "v2",
        "grpc-trace-bin", "AAARf5ZpQwlN/8FVe1axOPlaAQIdRU/Y8j0LAgE");

      GrpcClientRequestContextUtil.executeWithHeadersContext(requestHeaders, () -> {
        RequestContext requestContext = RequestContext.CURRENT.get();
        Assertions.assertEquals(Map.of("authorization", "v1", "a2", "v2",
          "grpc-trace-bin", "AAARf5ZpQwlN/8FVe1axOPlaAQIdRU/Y8j0LAgE"), requestContext.getRequestHeaders());
        return new Object();
      });
    } finally {
      ctx.detach(previous);
    }
  }


  @Test
  public void testExecuteInTenantIdContext() {
    Assertions.assertNull(RequestContext.CURRENT.get());
    Assertions.assertEquals(Optional.of(TENANT_ID),
        GrpcClientRequestContextUtil.executeInTenantContext(TENANT_ID,
            () -> RequestContext.CURRENT.get().getTenantId()));
  }

  @Test
  public void testExecuteInTenantIdContextForRunnable() {
    Assertions.assertNull(RequestContext.CURRENT.get());
    List<String> newList = new ArrayList<>();
    GrpcClientRequestContextUtil.executeInTenantContext(TENANT_ID,
        () -> {
          newList.add(RequestContext.CURRENT.get().getTenantId().get());
        });

    Assertions.assertEquals(1, newList.size());
    Assertions.assertEquals(TENANT_ID, newList.get(0));
  }

  @Test
  public void testExecuteWithHeadersContext() {
    Set<String> newSet = new HashSet<>();
    Map<String, String> headers = Map.of("a1", "v1", "a2", "v2");
    GrpcClientRequestContextUtil.executeWithHeadersContext(headers,
        () -> {
          newSet.addAll(
              RequestContext.CURRENT.get().getRequestHeaders().entrySet().stream()
                  .map((entry) -> entry.getKey() + "=" + entry.getValue())
                  .collect(Collectors.toUnmodifiableList())
          );
        });

    Assertions.assertEquals(2, newSet.size());
    Assertions.assertTrue(newSet.contains("a1=v1"));
    Assertions.assertTrue(newSet.contains("a2=v2"));
  }

  @Test
  public void testExecuteWithHeadersContextThrowsRuntimeExceptionWhenRunnableThrowsException() {
    Map<String, String> headers = Map.of("a1", "v1", "a2", "v2");
    Assertions.assertThrows(RuntimeException.class, () -> {
      GrpcClientRequestContextUtil.executeWithHeadersContext(headers,
          () -> {
            throw new Exception("test exception");
          });
    });
  }
}
