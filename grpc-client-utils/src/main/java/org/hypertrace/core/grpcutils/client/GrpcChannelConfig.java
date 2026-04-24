package org.hypertrace.core.grpcutils.client;

import io.grpc.ClientInterceptor;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class GrpcChannelConfig {
  Integer maxInboundMessageSize;

  @Singular List<ClientInterceptor> clientInterceptors;

  Map<String, Object> serviceConfig;
}
