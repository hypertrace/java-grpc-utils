package org.hypertrace.core.grpcutils.client;

import io.grpc.ClientInterceptor;
import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class GrpcChannelConfig {
  Integer maxInboundMessageSize;

  @Singular List<ClientInterceptor> clientInterceptors;
}
