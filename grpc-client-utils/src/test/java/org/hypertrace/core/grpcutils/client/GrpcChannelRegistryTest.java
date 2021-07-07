package org.hypertrace.core.grpcutils.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GrpcChannelRegistryTest {

  GrpcChannelRegistry channelRegistry;

  @BeforeEach
  void beforeEach() {
    this.channelRegistry = new GrpcChannelRegistry();
  }

  @Test
  void createsNewChannelsAsRequested() {
    assertNotNull(this.channelRegistry.forPlaintextAddress("foo", 1000));
  }

  @Test
  void reusesChannelsForDuplicateRequests() {
    Channel firstChannel = this.channelRegistry.forPlaintextAddress("foo", 1000);
    assertSame(firstChannel, this.channelRegistry.forPlaintextAddress("foo", 1000));
    Channel firstChannelSecure = this.channelRegistry.forSecureAddress("foo", 1000);
    assertSame(firstChannelSecure, this.channelRegistry.forSecureAddress("foo", 1000));
    assertNotSame(firstChannel, firstChannelSecure);
    assertNotSame(firstChannel, this.channelRegistry.forPlaintextAddress("foo", 1001));
    assertNotSame(firstChannel, this.channelRegistry.forPlaintextAddress("foo", 1001));
    assertNotSame(firstChannelSecure, this.channelRegistry.forSecureAddress("bar", 1000));
    assertNotSame(firstChannelSecure, this.channelRegistry.forSecureAddress("bar", 1000));
  }

  @Test
  void shutdownAllChannelsOnShutdown() {
    ManagedChannel firstChannel = this.channelRegistry.forPlaintextAddress("foo", 1000);
    ManagedChannel secondChannel = this.channelRegistry.forSecureAddress("foo", 1002);
    assertFalse(firstChannel.isShutdown());
    assertFalse(secondChannel.isShutdown());
    this.channelRegistry.shutdown();
    assertTrue(firstChannel.isShutdown());
    assertTrue(secondChannel.isShutdown());
  }

  @Test
  void throwsIfNewChannelRequestedAfterShutdown() {
    this.channelRegistry.shutdown();
    assertThrows(AssertionError.class, () -> this.channelRegistry.forPlaintextAddress("foo", 1000));
    assertThrows(AssertionError.class, () -> this.channelRegistry.forSecureAddress("foo", 1000));
  }
}
