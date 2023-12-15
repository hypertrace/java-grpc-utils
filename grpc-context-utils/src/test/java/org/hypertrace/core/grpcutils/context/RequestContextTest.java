package org.hypertrace.core.grpcutils.context;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.google.common.collect.ImmutableList;
import io.grpc.Metadata;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.hypertrace.core.grpcutils.context.RequestContext.RequestContextHeader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
    String requestId = requestContext.getRequestId().orElseThrow();
    assertDoesNotThrow(() -> UUID.fromString(requestId));
    assertEquals(Optional.of(TENANT_ID), requestContext.getTenantId());
    assertEquals(
        Optional.of(TENANT_ID), requestContext.get(RequestContextConstants.TENANT_ID_HEADER_KEY));
    assertEquals(
        Optional.of(requestId), requestContext.get(RequestContextConstants.REQUEST_ID_HEADER_KEY));
    assertEquals(
        Set.of(
            RequestContextConstants.TENANT_ID_HEADER_KEY,
            RequestContextConstants.REQUEST_ID_HEADER_KEY),
        requestContext.getAll().keySet());
  }

  @Test
  void testRolesArePropagatedInRequestContext() {
    List<String> expectedRoles = ImmutableList.of("super_user", "user", "billing_admin");
    String jwt =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE2MjEzNjM1OTcsIm"
            + "V4cCI6MTY1Mjg5OTU5NywiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIkdpdmVuTmFtZSI6Ik"
            + "pvaG5ueSIsIlN1cm5hbWUiOiJSb2NrZXQiLCJuYW1lIjoiSm9obm55IFJvY2tldCIsImVtYWlsIjoianJvY2tldEBleGFtcGxlLmNvbSIsIn"
            + "BpY3R1cmUiOiJ3d3cuZXhhbXBsZS5jb20iLCJyb2xlcyI6WyJzdXBlcl91c2VyIiwidXNlciIsImJpbGxpbmdfYWRtaW4iXX0.lEDjPPCjr-"
            + "Epv6pNslq-HK9vmxfstp1sY85GstlbU1I";

    RequestContext requestContext = new RequestContext();
    requestContext.add("authorization", "Bearer " + jwt);
    List<String> actualRoles = requestContext.getRoles("roles");
    assertEquals(expectedRoles, actualRoles);
  }

  @Test
  public void testMetadataKeys() {
    Metadata metadata = new Metadata();
    metadata.put(
        Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER),
        "Bearer Some-bearer-auth");
    metadata.put(RequestContextConstants.TENANT_ID_METADATA_KEY, "test-tenant-id");

    RequestContext requestContext = RequestContext.fromMetadata(metadata);

    Assertions.assertEquals(2, requestContext.getAll().size());
    // GRPC Metadata keys are lower cased during creation.
    Assertions.assertEquals("Bearer Some-bearer-auth", requestContext.get("authorization").get());
    Assertions.assertEquals(
        "test-tenant-id",
        requestContext.get(RequestContextConstants.TENANT_ID_METADATA_KEY.name()).get());

    metadata = new Metadata();
    metadata.put(
        Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER),
        "Bearer Some-bearer-auth");
    metadata.put(RequestContextConstants.TENANT_ID_METADATA_KEY, "test-tenant-id");
    metadata.put(
        Metadata.Key.of("x-some-other-header", Metadata.ASCII_STRING_MARSHALLER),
        "Some-other-header-val");

    requestContext = RequestContext.fromMetadata(metadata);

    Assertions.assertEquals(2, requestContext.getAll().size());
    Assertions.assertEquals("Bearer Some-bearer-auth", requestContext.get("authorization").get());
    Assertions.assertEquals(
        "test-tenant-id",
        requestContext.get(RequestContextConstants.TENANT_ID_METADATA_KEY.name()).get());

    // Test that header keys are lowercased
    metadata = new Metadata();
    metadata.put(
        Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
        "Bearer Some-bearer-auth-2");
    metadata.put(
        Metadata.Key.of("X-tenant-Id", Metadata.ASCII_STRING_MARSHALLER), "test-tenant-id-2");
    metadata.put(
        Metadata.Key.of("X-Some-Other-Header", Metadata.ASCII_STRING_MARSHALLER),
        "Some-other-header-val-2");

    requestContext = RequestContext.fromMetadata(metadata);

    Assertions.assertEquals(2, requestContext.getAll().size());
    Assertions.assertEquals("Bearer Some-bearer-auth-2", requestContext.get("authorization").get());
    Assertions.assertEquals(
        "test-tenant-id-2",
        requestContext.get(RequestContextConstants.TENANT_ID_METADATA_KEY.name()).get());

    metadata = new Metadata();
    metadata.put(
        Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
        "Bearer Some-bearer-auth-3");
    metadata.put(
        Metadata.Key.of("X-tenant-Id", Metadata.ASCII_STRING_MARSHALLER), "test-tenant-id-3");
    metadata.put(
        Metadata.Key.of("X-Some-Other-Header", Metadata.ASCII_STRING_MARSHALLER),
        "Some-other-header-val-3");
    metadata.put(
        Metadata.Key.of("grpc-trace-bin", Metadata.BINARY_BYTE_MARSHALLER),
        "AAARf5ZpQwlN/8FVe1axOPlaAQIdRU/Y8j0LAgE".getBytes());

    requestContext = RequestContext.fromMetadata(metadata);

    Assertions.assertEquals(3, requestContext.getAll().size());
    Assertions.assertEquals("Bearer Some-bearer-auth-3", requestContext.get("authorization").get());
    Assertions.assertEquals(
        "test-tenant-id-3",
        requestContext.get(RequestContextConstants.TENANT_ID_METADATA_KEY.name()).get());
    Assertions.assertEquals(
        "AAARf5ZpQwlN/8FVe1axOPlaAQIdRU/Y8j0LAgE", requestContext.get("grpc-trace-bin").get());
  }

  @Test
  void buildsTrailers() {
    RequestContext requestContext =
        RequestContext.forTenantId("test").put("other-header", "other-value");

    // Try building trailers and then request context from them.
    RequestContext requestContextFromBuiltTrailers =
        RequestContext.fromMetadata(requestContext.buildTrailers());

    // Should not be equal because other header is not a trailer so should be lost
    assertNotEquals(requestContext, requestContextFromBuiltTrailers);
    // Request ID and tenant ID should however be equal
    assertEquals(requestContext.getRequestId(), requestContextFromBuiltTrailers.getRequestId());
    assertEquals(requestContext.getTenantId(), requestContextFromBuiltTrailers.getTenantId());
  }

  @Test
  void canGetHeaderNames() {
    RequestContext requestContext =
        new RequestContext().put("first", "f-v").put("first", "f-v2").put("second", "s-v");

    assertEquals(Set.of("first", "second"), requestContext.getHeaderNames());
  }

  @Test
  void returnsEmptyIfNoHeaderWithName() {
    RequestContext requestContext = new RequestContext();

    assertEquals(Set.of(), requestContext.getHeaderNames());
    assertEquals(List.of(), requestContext.getAllHeaders());
    assertEquals(Optional.empty(), requestContext.getHeaderValue("test"));
    assertEquals(List.of(), requestContext.getAllHeaderValues("test"));
  }

  @Test
  void acceptsMultipleHeaderValues() {
    RequestContext requestContext = new RequestContext().put("first", "f-v").put("first", "f-v2");

    assertEquals(Optional.of("f-v"), requestContext.getHeaderValue("first"));
    assertEquals(List.of("f-v", "f-v2"), requestContext.getAllHeaderValues("first"));
  }

  @Test
  void ignoresButPreservesHeaderNameCase() {
    RequestContext requestContext = new RequestContext().put("first", "f-v").put("FIRST", "f-v2");

    assertEquals(Set.of("first"), requestContext.getHeaderNames());
    assertEquals(
        List.of(
            new RequestContextHeader("first", "f-v"), new RequestContextHeader("FIRST", "f-v2")),
        requestContext.getAllHeaders());

    assertEquals(Optional.of("f-v"), requestContext.getHeaderValue("FIRST"));
    assertEquals(Optional.of("f-v"), requestContext.getHeaderValue("First"));
    assertEquals(Optional.of("f-v"), requestContext.getHeaderValue("first"));
    assertEquals(List.of("f-v", "f-v2"), requestContext.getAllHeaderValues("FIRST"));
    assertEquals(List.of("f-v", "f-v2"), requestContext.getAllHeaderValues("First"));
    assertEquals(List.of("f-v", "f-v2"), requestContext.getAllHeaderValues("first"));
  }

  @Test
  void removesHeaders() {
    RequestContext requestContext = new RequestContext().put("first", "f-v").put("FIRST", "f-v2");

    assertEquals(List.of("f-v", "f-v2"), requestContext.removeHeader("First"));
    assertEquals(Set.of(), requestContext.getHeaderNames());
    assertEquals(List.of(), requestContext.getAllHeaders());
    assertEquals(Optional.empty(), requestContext.getHeaderValue("first"));
    assertEquals(List.of(), requestContext.getAllHeaderValues("FIRST"));
  }

  @Test
  void backwardsCompatibilityForAdd() {
    RequestContext requestContext = new RequestContext();
    requestContext.add("first", "f-v");
    requestContext.add("first", "f-v2");

    assertEquals(Set.of("first"), requestContext.getHeaderNames());
    assertEquals(
        List.of(new RequestContextHeader("first", "f-v2")), requestContext.getAllHeaders());
    assertEquals(Optional.of("f-v2"), requestContext.getHeaderValue("first"));
    assertEquals(List.of("f-v2"), requestContext.getAllHeaderValues("first"));
  }
}
