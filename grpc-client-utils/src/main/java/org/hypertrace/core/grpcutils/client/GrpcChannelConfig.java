package org.hypertrace.core.grpcutils.client;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GrpcChannelConfig {
  Integer maxInboundMessageSize;
}
