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

  public ManagedChannel forAddress(String host, int port) {
    assert !this.isShutdown;
    String channelId = this.getChannelId(host, port);
    return this.channelMap.computeIfAbsent(channelId, unused -> this.buildNewChannel(host, port));
  }

  private ManagedChannel buildNewChannel(String host, int port) {
    LOG.info("Creating new channel for {}:{}", host, port);
    return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
  }

  private String getChannelId(String host, int port) {
    return host + ":" + port;
  }

  public void shutdown() {
    channelMap.values().forEach(ManagedChannel::shutdown);
    this.isShutdown = true;
  }
}
