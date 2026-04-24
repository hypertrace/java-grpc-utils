package org.hypertrace.core.grpcutils.client;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GrpcServiceConfig {
  private static final String METHOD_CONFIG = "methodConfig";
  private static final String NAME = "name";
  private static final String RETRY_POLICY = "retryPolicy";

  GrpcRetryPolicy retryPolicy;

  Map<String, Object> toMap() {
    return Map.of(
        METHOD_CONFIG, List.of(Map.of(NAME, List.of(Map.of()), RETRY_POLICY, retryPolicy.toMap())));
  }
}
