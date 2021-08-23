package org.hypertrace.core.grpcutils.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
    return this.forPlaintextAddress(host, port);
  }

  public ManagedChannel forSecureAddress(String host, int port) {
    assert !this.isShutdown;
    String channelId = this.getChannelId(host, port, false);
    return this.channelMap.computeIfAbsent(
        channelId, unused -> this.buildNewChannel(host, port, false));
  }

  public ManagedChannel forPlaintextAddress(String host, int port) {
    assert !this.isShutdown;
    String channelId = this.getChannelId(host, port, true);
    return this.channelMap.computeIfAbsent(
        channelId, unused -> this.buildNewChannel(host, port, true));
  }

  private ManagedChannel buildNewChannel(String host, int port, boolean isPlaintext) {
    LOG.info("Creating new channel {}", this.getChannelId(host, port, isPlaintext));

    ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(host, port);
    if (isPlaintext) {
      builder.usePlaintext();
    }
    return builder.build();
  }

  private String getChannelId(String host, int port, boolean isPlaintext) {
    String securePrefix = isPlaintext ? "plaintext" : "secure";
    return securePrefix + ":" + host + ":" + port;
  }

  public void shutdown() {
    channelMap.forEach(this::fullyShutdownChannel);
    this.isShutdown = true;
  }

  private void fullyShutdownChannel(String channelId, ManagedChannel managedChannel) {
    LOG.info("Starting shutdown for channel [{}]", channelId);
    managedChannel.shutdown();
    if (this.waitForTermination(channelId, managedChannel)) {
      LOG.info("Shutdown channel successfully [{}]", channelId);
      return;
    }

    LOG.error("Shutting down channel [{}] forcefully", channelId);
    managedChannel.shutdownNow();
    if (this.waitForTermination(channelId, managedChannel)) {
      LOG.error("Forced channel [{}] shutdown successful", channelId);
    } else {
      LOG.error("Unable to force channel [{}] shutdown - giving up!", channelId);
    }
  }

  private boolean waitForTermination(String channelId, ManagedChannel managedChannel) {
    try {
      if (!managedChannel.awaitTermination(1, TimeUnit.MINUTES)) {
        LOG.error("Channel [{}] did not shut down after 1 minute", channelId);
      }
    } catch (InterruptedException ex) {
      LOG.error(
          "There has been an interruption during the shutdown process for channel [{}]", channelId);
      Thread.currentThread().interrupt();
    }
    return managedChannel.isTerminated();
  }
}
