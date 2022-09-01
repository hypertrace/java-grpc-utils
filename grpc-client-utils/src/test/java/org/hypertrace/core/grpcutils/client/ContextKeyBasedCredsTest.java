package org.hypertrace.core.grpcutils.client;

import static org.mockito.Mockito.mock;

import io.grpc.CallCredentials;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.Status;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.hypertrace.core.grpcutils.context.RequestContextConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ContextKeyBasedCredsTest {
  private static final String TENANT_ID = "test-tenant-id";
  private static final String TEST_AUTH_HEADER = "test-auth-header";

  @Test
  public void testApplyRequestMetadata_shouldApplyAllHeaders() {
    CallCredentials callCredentials =
        RequestContextClientCallCredsProviderFactory.getClientCallCredsProvider().get();

    Assertions.assertTrue(callCredentials instanceof ContextKeyBasedCreds);

    RequestContext requestContext = new RequestContext();

    requestContext.add(RequestContextConstants.TENANT_ID_HEADER_KEY, TENANT_ID);
    requestContext.add(RequestContextConstants.AUTHORIZATION_HEADER, TEST_AUTH_HEADER);
    requestContext.add("x-some-tenant-header", "v1");
    requestContext.add("grpc-trace-bin", "AAARf5ZpQwlN/8FVe1axOPlaAQIdRU/Y8j0LAgE");

    Context ctx = Context.current().withValue(RequestContext.CURRENT, requestContext);

    Context previous = ctx.attach();
    try {
      callCredentials.applyRequestMetadata(
          mock(CallCredentials.RequestInfo.class),
          mock(Executor.class),
          new CallCredentials.MetadataApplier() {
            @Override
            public void apply(Metadata headers) {
              Map<String, String> headersMap =
                  headers.keys().stream()
                      .collect(
                          Collectors.toUnmodifiableMap(
                              k -> k,
                              k -> {
                                String value;
                                if (k.toLowerCase().endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                                  value =
                                      new String(
                                          headers.get(
                                              Metadata.Key.of(k, Metadata.BINARY_BYTE_MARSHALLER)));
                                } else {
                                  value =
                                      headers.get(
                                          Metadata.Key.of(k, Metadata.ASCII_STRING_MARSHALLER));
                                }
                                return value;
                              }));

              // Should filter out the TENANT_ID_HEADER_KEY
              Assertions.assertEquals(
                  Map.of(
                      RequestContextConstants.TENANT_ID_HEADER_KEY,
                      TENANT_ID,
                      RequestContextConstants.AUTHORIZATION_HEADER,
                      TEST_AUTH_HEADER,
                      "x-some-tenant-header",
                      "v1",
                      "grpc-trace-bin",
                      "AAARf5ZpQwlN/8FVe1axOPlaAQIdRU/Y8j0LAgE"),
                  headersMap);
            }

            @Override
            public void fail(Status status) {
              Assertions.fail("Failed in CallCredentials.MetadataApplier");
            }
          });
    } finally {
      ctx.detach(previous);
    }
  }
}
