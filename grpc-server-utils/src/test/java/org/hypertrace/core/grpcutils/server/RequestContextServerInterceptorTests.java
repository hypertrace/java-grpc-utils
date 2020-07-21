package org.hypertrace.core.grpcutils.server;

import io.grpc.Metadata;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.hypertrace.core.grpcutils.context.RequestContextConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RequestContextServerInterceptorTests {
  @Test
  public void testMetadataKeys() {
    RequestContextServerInterceptor interceptor = new RequestContextServerInterceptor();
    Metadata metadata = new Metadata();
    metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer Some-bearer-auth");
    metadata.put(RequestContextConstants.TENANT_ID_METADATA_KEY, "test-tenant-id");

    RequestContext requestContext = interceptor.createRequestContextFromMetadata(metadata);

    Assertions.assertEquals(2, requestContext.getAll().size());
    // GRPC Metadata keys are lower cased during creation.
    Assertions.assertEquals("Bearer Some-bearer-auth", requestContext.get("authorization").get());
    Assertions.assertEquals("test-tenant-id", requestContext.get(RequestContextConstants.TENANT_ID_METADATA_KEY.name()).get());

    metadata = new Metadata();
    metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer Some-bearer-auth");
    metadata.put(RequestContextConstants.TENANT_ID_METADATA_KEY, "test-tenant-id");
    metadata.put(Metadata.Key.of("x-some-other-header", Metadata.ASCII_STRING_MARSHALLER), "Some-other-header-val");

    requestContext = interceptor.createRequestContextFromMetadata(metadata);

    Assertions.assertEquals(2, requestContext.getAll().size());
    Assertions.assertEquals("Bearer Some-bearer-auth", requestContext.get("authorization").get());
    Assertions.assertEquals("test-tenant-id", requestContext.get(RequestContextConstants.TENANT_ID_METADATA_KEY.name()).get());

    // Test that header keys are lowercased
    metadata = new Metadata();
    metadata.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer Some-bearer-auth-2");
    metadata.put(Metadata.Key.of("X-tenant-Id", Metadata.ASCII_STRING_MARSHALLER), "test-tenant-id-2");
    metadata.put(Metadata.Key.of("X-Some-Other-Header", Metadata.ASCII_STRING_MARSHALLER), "Some-other-header-val-2");

    requestContext = interceptor.createRequestContextFromMetadata(metadata);

    Assertions.assertEquals(2, requestContext.getAll().size());
    Assertions.assertEquals("Bearer Some-bearer-auth-2", requestContext.get("authorization").get());
    Assertions.assertEquals("test-tenant-id-2", requestContext.get(RequestContextConstants.TENANT_ID_METADATA_KEY.name()).get());


    metadata = new Metadata();
    metadata.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer Some-bearer-auth-3");
    metadata.put(Metadata.Key.of("X-tenant-Id", Metadata.ASCII_STRING_MARSHALLER), "test-tenant-id-3");
    metadata.put(Metadata.Key.of("X-Some-Other-Header", Metadata.ASCII_STRING_MARSHALLER), "Some-other-header-val-3");
    metadata.put(Metadata.Key.of("grpc-trace-bin", Metadata.BINARY_BYTE_MARSHALLER), "AAARf5ZpQwlN/8FVe1axOPlaAQIdRU/Y8j0LAgE".getBytes());

    requestContext = interceptor.createRequestContextFromMetadata(metadata);

    Assertions.assertEquals(3, requestContext.getAll().size());
    Assertions.assertEquals("Bearer Some-bearer-auth-3", requestContext.get("authorization").get());
    Assertions.assertEquals("test-tenant-id-3", requestContext.get(RequestContextConstants.TENANT_ID_METADATA_KEY.name()).get());
    Assertions.assertEquals("AAARf5ZpQwlN/8FVe1axOPlaAQIdRU/Y8j0LAgE", requestContext.get("grpc-trace-bin").get());
  }
}
