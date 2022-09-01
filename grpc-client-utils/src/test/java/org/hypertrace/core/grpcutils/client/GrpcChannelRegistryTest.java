package org.hypertrace.core.grpcutils.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.grpc.Channel;
import io.grpc.Deadline;
import io.grpc.Deadline.Ticker;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

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
  void setsMaxInboundMessageSizeConfig() {
    Channel channel =
        this.channelRegistry.forPlaintextAddress(
            "foo", 1000, GrpcChannelConfig.builder().maxInboundMessageSize(100).build());
    // same message size
    assertSame(
        channel,
        this.channelRegistry.forPlaintextAddress(
            "foo", 1000, GrpcChannelConfig.builder().maxInboundMessageSize(100).build()));
    // different message size
    assertNotSame(
        channel,
        this.channelRegistry.forPlaintextAddress(
            "foo", 1000, GrpcChannelConfig.builder().maxInboundMessageSize(200).build()));
  }

  @SuppressWarnings("rawtypes")
  @Test
  void shutdownAllChannelsOnShutdown() throws InterruptedException {
    this.channelRegistry = new GrpcChannelRegistry();
    try (MockedStatic<ManagedChannelBuilder> mockedBuilderStatic =
        Mockito.mockStatic(ManagedChannelBuilder.class)) {

      mockedBuilderStatic
          .when(() -> ManagedChannelBuilder.forAddress(anyString(), anyInt()))
          .thenAnswer(
              invocation -> {
                ManagedChannelBuilder<?> mockBuilder = mock(ManagedChannelBuilder.class);
                when(mockBuilder.intercept(List.of())).then(RETURNS_SELF);
                when(mockBuilder.build()).thenReturn(mock(ManagedChannel.class));
                return mockBuilder;
              });

      ManagedChannel firstChannel = this.channelRegistry.forPlaintextAddress("foo", 1000);
      ManagedChannel secondChannel = this.channelRegistry.forSecureAddress("foo", 1002);

      verifyNoInteractions(firstChannel);
      verifyNoInteractions(secondChannel);

      // First channel shuts down successfully
      when(firstChannel.isTerminated()).thenReturn(true);
      // Second does not
      when(secondChannel.isTerminated()).thenReturn(false);

      Ticker mockTicker = mock(Ticker.class);

      when(mockTicker.nanoTime()).thenReturn(0L);

      // Wait for 10ms (test clock fixed at 0)
      this.channelRegistry.shutdown(Deadline.after(10, TimeUnit.MILLISECONDS, mockTicker));

      // First channel requests shutdown, waits, succeeds and checks result
      InOrder firstChannelVerifier = inOrder(firstChannel);
      firstChannelVerifier.verify(firstChannel).shutdown();
      firstChannelVerifier.verify(firstChannel).awaitTermination(10, TimeUnit.MILLISECONDS);
      firstChannelVerifier.verify(firstChannel).isTerminated();
      firstChannelVerifier.verifyNoMoreInteractions();

      // Second channel requests shutdown, waits, fails, checks result, forces shutdown, waits,
      // fails and checks result again
      InOrder secondChannelVerifier = inOrder(secondChannel);
      secondChannelVerifier.verify(secondChannel).shutdown();
      secondChannelVerifier.verify(secondChannel).awaitTermination(10, TimeUnit.MILLISECONDS);
      secondChannelVerifier.verify(secondChannel).isTerminated();
      secondChannelVerifier.verify(secondChannel).shutdownNow();
      // hardcoded additional 5s for force shutdown
      secondChannelVerifier.verify(secondChannel).awaitTermination(5010, TimeUnit.MILLISECONDS);
      secondChannelVerifier.verify(secondChannel).isTerminated();
    }
  }

  @Test
  void throwsIfNewChannelRequestedAfterShutdown() {
    this.channelRegistry.shutdown();
    assertThrows(AssertionError.class, () -> this.channelRegistry.forPlaintextAddress("foo", 1000));
    assertThrows(AssertionError.class, () -> this.channelRegistry.forSecureAddress("foo", 1000));
  }

  @Test
  void copyConstructorReusesExistingChannels() {
    GrpcChannelRegistry firstRegistry = new GrpcChannelRegistry();

    Channel firstChannel = firstRegistry.forSecureAddress("foo", 1000);

    assertSame(firstChannel, new GrpcChannelRegistry(firstRegistry).forSecureAddress("foo", 1000));
  }
}
