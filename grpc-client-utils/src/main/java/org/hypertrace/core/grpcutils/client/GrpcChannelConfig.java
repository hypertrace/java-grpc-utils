package org.hypertrace.core.grpcutils.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrpcChannelConfig {
  private Integer maxInboundMessageSize;
}
