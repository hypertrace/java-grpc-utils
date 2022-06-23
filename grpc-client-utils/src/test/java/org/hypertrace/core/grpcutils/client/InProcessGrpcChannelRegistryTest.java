package org.hypertrace.core.grpcutils.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.grpc.Channel;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InProcessGrpcChannelRegistryTest {
  InProcessGrpcChannelRegistry channelRegistry;

  @BeforeEach
  void beforeEach() {
    this.channelRegistry = new InProcessGrpcChannelRegistry();
  }

  @Test
  void createsNewChannelsAsRequested() {
    // Regular channel
    assertEquals("foo:1000", this.channelRegistry.forPlaintextAddress("foo", 1000).authority());
    assertEquals("foo:1000", this.channelRegistry.forSecureAddress("foo", 1000).authority());
    // In process runs on localhost
    assertEquals("localhost", this.channelRegistry.forName("inprocess-name").authority());
  }

  @Test
  void reusesInProcessChannels() {
    assertSame(
        this.channelRegistry.forName("inprocess-name"),
        this.channelRegistry.forName("inprocess-name"));

    // But not if their config differs
    assertNotSame(
        this.channelRegistry.forName("inprocess-name"),
        this.channelRegistry.forName(
            "inprocess-name", GrpcChannelConfig.builder().maxInboundMessageSize(100).build()));
  }

  @Test
  void overridesAuthorityByConfig() {
    this.channelRegistry = new InProcessGrpcChannelRegistry(Map.of("foo:1000", "inprocess-name"));
    assertSame(
        this.channelRegistry.forSecureAddress("foo", 1000),
        this.channelRegistry.forName("inprocess-name"));
    assertSame(
        this.channelRegistry.forPlaintextAddress("foo", 1000),
        this.channelRegistry.forName("inprocess-name"));
    // Also works with custom config

    assertSame(
        this.channelRegistry.forSecureAddress(
            "foo", 1000, GrpcChannelConfig.builder().maxInboundMessageSize(100).build()),
        this.channelRegistry.forName(
            "inprocess-name", GrpcChannelConfig.builder().maxInboundMessageSize(100).build()));

    // but custom config shouldn't match the default config ones
    assertNotSame(
        this.channelRegistry.forSecureAddress("foo", 1000),
        this.channelRegistry.forName(
            "inprocess-name", GrpcChannelConfig.builder().maxInboundMessageSize(100).build()));
  }

  @Test
  void copyConstructorReusesExistingChannelsAndOverrides() {
    InProcessGrpcChannelRegistry firstRegistry =
        new InProcessGrpcChannelRegistry(Map.of("foo:1000", "inprocess-name"));

    Channel firstChannel = firstRegistry.forName("inprocess-name");

    assertSame(
        firstChannel,
        new InProcessGrpcChannelRegistry(firstRegistry).forSecureAddress("foo", 1000));
  }
}
