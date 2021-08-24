package org.hypertrace.core.grpcutils.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
    this.channelRegistry = new GrpcChannelRegistry(Clock.systemUTC());
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
  void shutdownAllChannelsOnShutdown() throws InterruptedException {
    this.channelRegistry =
        new GrpcChannelRegistry(Clock.fixed(Instant.ofEpochMilli(0), ZoneOffset.UTC));
    try (MockedStatic<ManagedChannelBuilder> mockedBuilderStatic =
        Mockito.mockStatic(ManagedChannelBuilder.class)) {

      mockedBuilderStatic
          .when(() -> ManagedChannelBuilder.forAddress(anyString(), anyInt()))
          .thenAnswer(
              invocation -> {
                ManagedChannelBuilder<?> mockBuilder = mock(ManagedChannelBuilder.class);
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

      // Wait for 10ms (test clock fixed at 0)
      this.channelRegistry.shutdown(Instant.ofEpochMilli(10));

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
      // hardcoded 5s for force shutdown
      secondChannelVerifier.verify(secondChannel).awaitTermination(5000, TimeUnit.MILLISECONDS);
      secondChannelVerifier.verify(secondChannel).isTerminated();
    }
  }

  @Test
  void throwsIfNewChannelRequestedAfterShutdown() {
    this.channelRegistry.shutdown();
    assertThrows(AssertionError.class, () -> this.channelRegistry.forPlaintextAddress("foo", 1000));
    assertThrows(AssertionError.class, () -> this.channelRegistry.forSecureAddress("foo", 1000));
  }
}
