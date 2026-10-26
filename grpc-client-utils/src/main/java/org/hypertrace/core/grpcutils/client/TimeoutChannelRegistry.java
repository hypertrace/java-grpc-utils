package org.hypertrace.core.grpcutils.client;

import com.typesafe.config.Config;
import java.time.Duration;

public final class TimeoutChannelRegistry {
  private static final String TIMEOUT_PATH = "grpc.client.timeout";
  private static final String ENABLED_KEY = "enabled";
  private static final String DEFAULT_TIMEOUT_KEY = "defaultTimeout";

  public static GrpcChannelRegistry wrap(GrpcChannelRegistry registry, Config config) {
    if (registry == null || config == null || !config.hasPath(TIMEOUT_PATH)) {
      return registry;
    }

    Config timeoutConfig = config.getConfig(TIMEOUT_PATH);
    if (!timeoutConfig.hasPath(ENABLED_KEY) || !timeoutConfig.getBoolean(ENABLED_KEY)) {
      return registry;
    }

    // An enabled block with no timeout must fail at startup rather than silently apply no deadline.
    Duration timeout = timeoutConfig.getDuration(DEFAULT_TIMEOUT_KEY);
    return InterceptingChannelRegistry.wrap(registry, new DefaultTimeoutClientInterceptor(timeout));
  }

  private TimeoutChannelRegistry() {}
}
