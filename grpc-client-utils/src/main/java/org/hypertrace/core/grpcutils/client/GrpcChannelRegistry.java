package org.hypertrace.core.grpcutils.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcChannelRegistry {
  private static final Logger LOG = LoggerFactory.getLogger(GrpcChannelRegistry.class);
  private final Map<String, ManagedChannel> channelMap = new ConcurrentHashMap<>();
  private volatile boolean isShutdown = false;

  /**
   * Use either {@link #forSecureAddress(String, int)} or {@link #forPlaintextAddress(String, int)}
   */
  @Deprecated
  public ManagedChannel forAddress(String host, int port) {
    return this.forPlaintextAddress(host, port, new GrpcChannelConfig());
  }

  public ManagedChannel forSecureAddress(String host, int port) {
    return forSecureAddress(host, port, new GrpcChannelConfig());
  }

  public ManagedChannel forSecureAddress(String host, int port, GrpcChannelConfig config) {
    assert !this.isShutdown;
    String channelId = this.getChannelId(host, port, false);
    return this.channelMap.computeIfAbsent(
        channelId, unused -> this.buildNewChannel(host, port, false, config));
  }

  public ManagedChannel forPlaintextAddress(String host, int port) {
    return forPlaintextAddress(host, port, new GrpcChannelConfig());
  }

  public ManagedChannel forPlaintextAddress(String host, int port, GrpcChannelConfig config) {
    assert !this.isShutdown;
    String channelId = this.getChannelId(host, port, true);
    return this.channelMap.computeIfAbsent(
        channelId, unused -> this.buildNewChannel(host, port, true, config));
  }

  private ManagedChannel buildNewChannel(
      String host, int port, boolean isPlaintext, GrpcChannelConfig config) {
    LOG.info("Creating new channel {}", this.getChannelId(host, port, isPlaintext));

    ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(host, port);
    if (isPlaintext) {
      builder.usePlaintext();
    }

    if (config.getMaxInboundMessageSize() != null) {
      builder.maxInboundMessageSize(config.getMaxInboundMessageSize());
    }
    return builder.build();
  }

  private String getChannelId(String host, int port, boolean isPlaintext) {
    String securePrefix = isPlaintext ? "plaintext" : "secure";
    return securePrefix + ":" + host + ":" + port;
  }

  public void shutdown() {
    channelMap.values().forEach(ManagedChannel::shutdown);
    this.isShutdown = true;
  }
}
