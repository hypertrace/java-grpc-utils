package org.hypertrace.core.grpcutils.client;

import com.typesafe.config.Config;
import java.time.Duration;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimeoutChannelRegistry {
  private static final String TIMEOUT_PATH = "grpc.client.timeout";
  private static final String ENABLED_KEY = "enabled";
  private static final String DEFAULT_TIMEOUT_KEY = "defaultTimeout";

  public static GrpcChannelRegistry wrap(final GrpcChannelRegistry registry, final Config config) {
    if (registry == null || config == null || !config.hasPath(TIMEOUT_PATH)) {
      log.debug("grpc.client.timeout is absent; leaving the channel registry unchanged");
      return registry;
    }

    final Config timeoutConfig = config.getConfig(TIMEOUT_PATH);
    if (!timeoutConfig.hasPath(ENABLED_KEY) || !timeoutConfig.getBoolean(ENABLED_KEY)) {
      log.debug("grpc.client.timeout is disabled; leaving the channel registry unchanged");
      return registry;
    }

    // An enabled block with no timeout must fail at startup rather than silently apply no deadline.
    final Duration timeout = timeoutConfig.getDuration(DEFAULT_TIMEOUT_KEY);
    return InterceptingChannelRegistry.wrap(registry, new DefaultTimeoutClientInterceptor(timeout));
  }
}
