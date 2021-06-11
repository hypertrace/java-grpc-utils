package org.hypertrace.core.grpcutils.context;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for {@link RequestContext} and utility methods in it. */
public class RequestContextTest {
  private static final String TENANT_ID = "example-tenant-id";
  private static final String TEST_AUTH_HEADER = "Bearer sample-auth-header";

  @Test
  void testTenantId() {
    RequestContext requestContext = new RequestContext();
    requestContext.add(RequestContextConstants.TENANT_ID_HEADER_KEY, TENANT_ID);
    Optional<String> tenantId = requestContext.getTenantId();
    assertEquals(Optional.of(TENANT_ID), tenantId);

    requestContext = new RequestContext();
    tenantId = requestContext.getTenantId();
    assertEquals(Optional.empty(), tenantId);
  }

  @Test
  void testGetRequestHeaders() {
    RequestContext requestContext = new RequestContext();
    requestContext.add(RequestContextConstants.AUTHORIZATION_HEADER, TEST_AUTH_HEADER);
    requestContext.add("x-some-tenant-header", "v1");

    Map<String, String> requestHeaders = requestContext.getRequestHeaders();

    assertEquals(
        Map.of(
            RequestContextConstants.AUTHORIZATION_HEADER,
            TEST_AUTH_HEADER,
            "x-some-tenant-header",
            "v1"),
        requestHeaders);
  }

  @Test
  void testCreateForTenantId() {
    RequestContext requestContext = RequestContext.forTenantId(TENANT_ID);
    assertEquals(Optional.of(TENANT_ID), requestContext.getTenantId());
    assertEquals(
        Optional.of(TENANT_ID), requestContext.get(RequestContextConstants.TENANT_ID_HEADER_KEY));
    assertEquals(
        Map.of(RequestContextConstants.TENANT_ID_HEADER_KEY, TENANT_ID), requestContext.getAll());
  }

  @Test
  void testRolesArePropagatedInRequestContext() {
    Set<String> expectedRoles = ImmutableSet.of("billing_admin", "user", "traceable");
    String jwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE2MjEzNjM1OTcsIm" +
        "V4cCI6MTY1Mjg5OTU5NywiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIkdpdmVuTmFtZSI6Ik" +
        "pvaG5ueSIsIlN1cm5hbWUiOiJSb2NrZXQiLCJuYW1lIjoiSm9obm55IFJvY2tldCIsImVtYWlsIjoianJvY2tldEBleGFtcGxlLmNvbSIsIn" +
        "BpY3R1cmUiOiJ3d3cuZXhhbXBsZS5jb20iLCJodHRwczovL3RyYWNlYWJsZS5haS9yb2xlcyI6WyJ0cmFjZWFibGUiLCJ1c2VyIiwiYmlsbG" +
        "luZ19hZG1pbiJdfQ.xdWar7cgJ_5V3SgECanVtBMhxJGb-DbeIfrKSpAQLJM";

    RequestContext requestContext = new RequestContext();
    requestContext.add("authorization", "Bearer " + jwt);
    Set<String> actualRoles = requestContext.getRoles();
    assertEquals(expectedRoles, actualRoles);
  }
}
