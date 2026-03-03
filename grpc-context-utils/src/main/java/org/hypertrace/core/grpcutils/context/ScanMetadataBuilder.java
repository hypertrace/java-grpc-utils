package org.hypertrace.core.grpcutils.context;

import static org.hypertrace.core.grpcutils.context.RequestContextConstants.CTX_SCAN_ID_HEADER_KEY;
import static org.hypertrace.core.grpcutils.context.RequestContextConstants.REQUEST_ID_HEADER_KEY;
import static org.hypertrace.core.grpcutils.context.RequestContextConstants.TENANT_ID_HEADER_KEY;

import io.grpc.Metadata;

public class ScanMetadataBuilder {

  public static Metadata build(String tenantId, String requestId, String scanId) {
    Metadata metadata = new Metadata();
    metadata.put(Metadata.Key.of(TENANT_ID_HEADER_KEY, Metadata.ASCII_STRING_MARSHALLER), tenantId);
    metadata.put(
        Metadata.Key.of(REQUEST_ID_HEADER_KEY, Metadata.ASCII_STRING_MARSHALLER), requestId);
    metadata.put(Metadata.Key.of(CTX_SCAN_ID_HEADER_KEY, Metadata.ASCII_STRING_MARSHALLER), scanId);
    return metadata;
  }
}
