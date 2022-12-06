package org.hypertrace.core.grpcutils.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

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

  @Test
  void registersRegistryInterceptors() {
    try (MockedStatic<InProcessChannelBuilder> mockedBuilderStatic =
        Mockito.mockStatic(InProcessChannelBuilder.class)) {
      InProcessChannelBuilder mockBuilder = mock(InProcessChannelBuilder.class);
      ManagedChannel mockChannel = mock(ManagedChannel.class);

      mockedBuilderStatic
          .when(() -> InProcessChannelBuilder.forName("test"))
          .thenAnswer(
              invocation -> {
                when(mockBuilder.intercept(anyList())).then(RETURNS_SELF);
                when(mockBuilder.intercept(any(ClientInterceptor.class))).then(RETURNS_SELF);
                when(mockBuilder.build()).thenReturn(mockChannel);
                return mockBuilder;
              });

      ClientInterceptor mockInterceptor1 = mock(ClientInterceptor.class);
      ClientInterceptor mockInterceptor2 = mock(ClientInterceptor.class);
      this.channelRegistry =
          new InProcessGrpcChannelRegistry(
              Map.of("host:1000", "test"),
              GrpcRegistryConfig.builder()
                  .defaultInterceptor(mockInterceptor1)
                  .defaultInterceptor(mockInterceptor2)
                  .build());

      assertSame(mockChannel, this.channelRegistry.forName("test"));
      assertSame(mockChannel, this.channelRegistry.forPlaintextAddress("host", 1000));
      verify(mockBuilder, times(1)).intercept(mockInterceptor1);
      verify(mockBuilder, times(1)).intercept(mockInterceptor2);
    }
  }
}
