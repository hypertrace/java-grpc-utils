package org.hypertrace.core.grpcutils.client;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcChannelRegistry {
  private static final Logger LOG = LoggerFactory.getLogger(GrpcChannelRegistry.class);
  private final Map<String, ManagedChannel> channelMap = new ConcurrentHashMap<>();
  private final Clock clock;
  private volatile boolean isShutdown = false;

  @Deprecated
  public GrpcChannelRegistry() {
    this(Clock.systemUTC());
  }

  public GrpcChannelRegistry(Clock clock) {
    this.clock = clock;
  }

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
    this.shutdown(this.clock.instant().plus(1, MINUTES));
  }

  public void shutdown(Instant deadline) {
    channelMap.forEach(this::initiateChannelShutdown);
    channelMap.keySet().stream()
        .filter(channelId -> !this.waitForGracefulShutdown(channelId, deadline))
        .forEach(this::forceShutdown);

    this.isShutdown = true;
    this.channelMap.clear();
  }

  private void initiateChannelShutdown(String channelId, ManagedChannel managedChannel) {
    LOG.info("Starting shutdown for channel [{}]", channelId);
    managedChannel.shutdown();
  }

  private boolean waitForGracefulShutdown(String channelId, Instant deadline) {
    boolean successfullyShutdown = this.waitForTermination(channelId, deadline);
    if (successfullyShutdown) {
      LOG.info("Shutdown channel successfully [{}]", channelId);
    }
    return successfullyShutdown;
  }

  private void forceShutdown(String channelId) {
    LOG.error("Shutting down channel [{}] forcefully", channelId);
    this.channelMap.get(channelId).shutdownNow();
    Instant forceShutdownDeadline = this.clock.instant().plus(5, SECONDS);
    if (this.waitForTermination(channelId, forceShutdownDeadline)) {
      LOG.error("Forced channel [{}] shutdown successful", channelId);
    } else {
      LOG.error("Unable to force channel [{}] shutdown in 5s - giving up!", channelId);
    }
  }

  private boolean waitForTermination(String channelId, Instant deadline) {
    ManagedChannel managedChannel = this.channelMap.get(channelId);
    long millisRemaining = Math.max(0, deadline.toEpochMilli() - this.clock.millis());
    try {
      if (!managedChannel.awaitTermination(millisRemaining, TimeUnit.MILLISECONDS)) {
        LOG.error("Channel [{}] did not shut down after waiting", channelId);
      }
    } catch (InterruptedException ex) {
      LOG.error(
          "There has been an interruption while waiting for channel [{}] to shutdown", channelId);
      Thread.currentThread().interrupt();
    }
    return managedChannel.isTerminated();
  }
}
