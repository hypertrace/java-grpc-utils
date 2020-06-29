package org.hypertrace.core.grpcutils.context;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RequestContext} and utility methods in it.
 */
public class RequestContextTest {
  private static final String TENANT_ID = "example-tenant-id";
  private static final String TEST_AUTH_HEADER = "Bearer sample-auth-header";

  @Test
  public void testTenantId() {
    RequestContext requestContext = new RequestContext();
    requestContext.add(RequestContextConstants.TENANT_ID_HEADER_KEY, TENANT_ID);
    Optional<String> tenantId = requestContext.getTenantId();
    Assertions.assertEquals(Optional.of(TENANT_ID), tenantId);

    requestContext = new RequestContext();
    tenantId = requestContext.getTenantId();
    Assertions.assertEquals(Optional.empty(), tenantId);
  }

  @Test
  public void testGetRequestHeaders() {
    RequestContext requestContext = new RequestContext();
    requestContext.add(RequestContextConstants.AUTHORIZATION_HEADER, TEST_AUTH_HEADER);
    requestContext.add("x-some-tenant-header", "v1");

    Map<String, String> requestHeaders = requestContext.getRequestHeaders();

    Assertions.assertEquals(
        Map.of(
            RequestContextConstants.AUTHORIZATION_HEADER, TEST_AUTH_HEADER,
            "x-some-tenant-header", "v1"
        ),
        requestHeaders);
  }
}
