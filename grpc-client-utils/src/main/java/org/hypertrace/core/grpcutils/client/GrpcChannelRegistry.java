package org.hypertrace.core.grpcutils.client;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import io.grpc.Deadline;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Map;
import java.util.Objects;
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
    return this.forPlaintextAddress(host, port, GrpcChannelConfig.builder().build());
  }

  public ManagedChannel forSecureAddress(String host, int port) {
    return forSecureAddress(host, port, GrpcChannelConfig.builder().build());
  }

  public ManagedChannel forSecureAddress(String host, int port, GrpcChannelConfig config) {
    assert !this.isShutdown;
    String channelId = this.getChannelId(host, port, false, config);
    return this.channelMap.computeIfAbsent(
        channelId, unused -> this.buildNewChannel(host, port, false, config));
  }

  public ManagedChannel forPlaintextAddress(String host, int port) {
    return forPlaintextAddress(host, port, GrpcChannelConfig.builder().build());
  }

  public ManagedChannel forPlaintextAddress(String host, int port, GrpcChannelConfig config) {
    assert !this.isShutdown;
    String channelId = this.getChannelId(host, port, true, config);
    return this.channelMap.computeIfAbsent(
        channelId, unused -> this.buildNewChannel(host, port, true, config));
  }

  private ManagedChannel buildNewChannel(
      String host, int port, boolean isPlaintext, GrpcChannelConfig config) {
    LOG.info("Creating new channel {}", this.getChannelId(host, port, isPlaintext, config));

    ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(host, port);
    if (isPlaintext) {
      builder.usePlaintext();
    }

    if (config.getMaxInboundMessageSize() != null) {
      builder.maxInboundMessageSize(config.getMaxInboundMessageSize());
    }
    return builder.build();
  }

  private String getChannelId(
      String host, int port, boolean isPlaintext, GrpcChannelConfig config) {
    String securePrefix = isPlaintext ? "plaintext" : "secure";
    return securePrefix + ":" + host + ":" + port + ":" + Objects.hash(config);
  }

  /**
   * Shuts down channels using a default deadline of 1 minute.
   *
   * @see #shutdown(Deadline)
   */
  public void shutdown() {
    this.shutdown(Deadline.after(1, TimeUnit.MINUTES));
  }

  /**
   * Attempts to perform an orderly shutdown of all registered channels before the provided
   * deadline, else falling back to a forceful shutdown. The call waits for all shutdowns to
   * complete. More specifically, we go through three shutdown phases.
   *
   * <ol>
   *   <li>First, we request an orderly shutdown across all registered channels. At this point, no
   *       new calls will be accepted, but in-flight calls will be given a chance to complete before
   *       shutting down.
   *   <li>Next, we sequentially wait for each channel to complete. Although sequential, each
   *       channel will wait no longer than the provided deadline.
   *   <li>For any channels that have not shutdown successfully after the previous phase, we will
   *       forcefully terminate it, cancelling any pending calls. Each channel is given up to 5
   *       seconds for forceful termination, but should complete close to instantly.
   * </ol>
   *
   * Upon completion, the registry is moved to a shutdown state and the channel references are
   * cleared. Attempting to reference any channels from the registry at this point will result in an
   * error.
   *
   * @param deadline Deadline for all channels to complete graceful shutdown.
   */
  public void shutdown(Deadline deadline) {
    channelMap.forEach(this::initiateChannelShutdown);
    channelMap.keySet().stream()
        .filter(channelId -> !this.waitForGracefulShutdown(channelId, deadline))
        .forEach(channelId -> this.forceShutdown(channelId, deadline.offset(5, TimeUnit.SECONDS)));

    this.isShutdown = true;
    this.channelMap.clear();
  }

  private void initiateChannelShutdown(String channelId, ManagedChannel managedChannel) {
    LOG.info("Starting shutdown for channel [{}]", channelId);
    managedChannel.shutdown();
  }

  private boolean waitForGracefulShutdown(String channelId, Deadline deadline) {
    boolean successfullyShutdown = this.waitForTermination(channelId, deadline);
    if (successfullyShutdown) {
      LOG.info("Shutdown channel successfully [{}]", channelId);
    }
    return successfullyShutdown;
  }

  private void forceShutdown(String channelId, Deadline deadline) {
    LOG.error("Shutting down channel [{}] forcefully", channelId);
    this.channelMap.get(channelId).shutdownNow();
    if (this.waitForTermination(channelId, deadline)) {
      LOG.error("Forced channel [{}] shutdown successful", channelId);
    } else {
      LOG.error("Unable to force channel [{}] shutdown in 5s - giving up!", channelId);
    }
  }

  private boolean waitForTermination(String channelId, Deadline deadline) {
    ManagedChannel managedChannel = this.channelMap.get(channelId);
    try {
      if (!managedChannel.awaitTermination(deadline.timeRemaining(MILLISECONDS), MILLISECONDS)) {
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
