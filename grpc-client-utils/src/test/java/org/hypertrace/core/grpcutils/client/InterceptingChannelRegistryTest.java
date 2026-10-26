package org.hypertrace.core.grpcutils.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Deadline;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.Marshaller;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InterceptingChannelRegistryTest {

  private static final MethodDescriptor<String, String> METHOD =
      MethodDescriptor.<String, String>newBuilder()
          .setType(MethodDescriptor.MethodType.UNARY)
          .setFullMethodName("test.Service/Method")
          .setRequestMarshaller(new StringMarshaller())
          .setResponseMarshaller(new StringMarshaller())
          .build();

  private final List<MethodDescriptor<?, ?>> interceptedMethods = new ArrayList<>();
  private final ClientInterceptor recordingInterceptor =
      new ClientInterceptor() {
        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
          interceptedMethods.add(method);
          return next.newCall(method, callOptions);
        }
      };

  private GrpcChannelRegistry delegate;
  private ManagedChannel rawChannel;
  private GrpcChannelRegistry wrapper;

  @BeforeEach
  void beforeEach() {
    this.delegate = mock(GrpcChannelRegistry.class);
    this.rawChannel = mock(ManagedChannel.class);
    this.wrapper = InterceptingChannelRegistry.wrap(this.delegate, this.recordingInterceptor);
  }

  @Test
  void wrapReturnsDistinctRegistry() {
    assertNotSame(this.delegate, this.wrapper);
    assertThrows(
        NullPointerException.class,
        () -> InterceptingChannelRegistry.wrap(null, this.recordingInterceptor));
    assertThrows(
        NullPointerException.class, () -> InterceptingChannelRegistry.wrap(this.delegate, null));
  }

  @Test
  void plaintextAndSecureChannelsCarryTheInterceptor() {
    GrpcChannelConfig config = GrpcChannelConfig.builder().build();
    ManagedChannel secureRaw = mock(ManagedChannel.class);
    when(this.delegate.forPlaintextAddress("host", 1)).thenReturn(this.rawChannel);
    when(this.delegate.forPlaintextAddress("host", 2, config)).thenReturn(this.rawChannel);
    when(this.delegate.forSecureAddress("host", 3)).thenReturn(secureRaw);
    when(this.delegate.forSecureAddress("host", 4, config)).thenReturn(secureRaw);

    List<ManagedChannel> channels =
        List.of(
            this.wrapper.forPlaintextAddress("host", 1),
            this.wrapper.forPlaintextAddress("host", 2, config),
            this.wrapper.forSecureAddress("host", 3),
            this.wrapper.forSecureAddress("host", 4, config));

    channels.forEach(channel -> channel.newCall(METHOD, CallOptions.DEFAULT));

    assertEquals(List.of(METHOD, METHOD, METHOD, METHOD), this.interceptedMethods);
    verify(this.rawChannel, times(2)).newCall(METHOD, CallOptions.DEFAULT);
    verify(secureRaw, times(2)).newCall(METHOD, CallOptions.DEFAULT);
  }

  @Test
  void repeatedLookupReturnsTheSameInterceptedChannel() {
    when(this.delegate.forPlaintextAddress("host", 1)).thenReturn(this.rawChannel);

    ManagedChannel first = this.wrapper.forPlaintextAddress("host", 1);

    assertNotSame(this.rawChannel, first);
    assertSame(first, this.wrapper.forPlaintextAddress("host", 1));
  }

  @Test
  void forAddressUsesThePlaintextOverride() {
    GrpcChannelConfig config = GrpcChannelConfig.builder().build();
    when(this.delegate.forPlaintextAddress("host", 1, config)).thenReturn(this.rawChannel);

    ManagedChannel channel = this.wrapper.forAddress("host", 1);
    channel.newCall(METHOD, CallOptions.DEFAULT);

    verify(this.delegate).forPlaintextAddress("host", 1, config);
    verify(this.delegate, never()).forAddress(anyString(), anyInt());
    assertEquals(List.of(METHOD), this.interceptedMethods);
  }

  @Test
  void shutdownMethodsReturnTheWrapper() {
    when(this.delegate.forPlaintextAddress("host", 1)).thenReturn(this.rawChannel);
    ManagedChannel channel = this.wrapper.forPlaintextAddress("host", 1);

    assertSame(channel, channel.shutdown());
    assertSame(channel, channel.shutdownNow());

    verify(this.rawChannel).shutdown();
    verify(this.rawChannel).shutdownNow();
  }

  @Test
  void registryShutdownMethodsForwardToTheDelegate() {
    Deadline deadline = Deadline.after(1, TimeUnit.SECONDS);
    when(this.delegate.isShutdown()).thenReturn(true);

    this.wrapper.shutdown();
    this.wrapper.shutdown(deadline);

    verify(this.delegate).shutdown();
    verify(this.delegate).shutdown(deadline);
    assertTrue(this.wrapper.isShutdown());
  }

  private static final class StringMarshaller implements Marshaller<String> {
    @Override
    public InputStream stream(String value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String parse(InputStream stream) {
      throw new UnsupportedOperationException();
    }
  }
}
