package org.hypertrace.core.grpcutils.client;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.experimental.Delegate;

public final class InterceptingChannelRegistry extends GrpcChannelRegistry {

  public static GrpcChannelRegistry wrap(
      GrpcChannelRegistry registry, ClientInterceptor interceptor) {
    return new InterceptingChannelRegistry(
        Objects.requireNonNull(registry), Objects.requireNonNull(interceptor));
  }

  @Delegate(excludes = ForAddress.class)
  private final GrpcChannelRegistry delegate;

  private final ClientInterceptor interceptor;
  private final Map<ManagedChannel, ManagedChannel> interceptedChannels =
      Collections.synchronizedMap(new IdentityHashMap<>());

  private InterceptingChannelRegistry(GrpcChannelRegistry delegate, ClientInterceptor interceptor) {
    this.delegate = delegate;
    this.interceptor = interceptor;
  }

  @Override
  public ManagedChannel forSecureAddress(String host, int port) {
    return this.intercept(this.delegate.forSecureAddress(host, port));
  }

  @Override
  public ManagedChannel forSecureAddress(String host, int port, GrpcChannelConfig config) {
    return this.intercept(this.delegate.forSecureAddress(host, port, config));
  }

  @Override
  public ManagedChannel forPlaintextAddress(String host, int port) {
    return this.intercept(this.delegate.forPlaintextAddress(host, port));
  }

  @Override
  public ManagedChannel forPlaintextAddress(String host, int port, GrpcChannelConfig config) {
    return this.intercept(this.delegate.forPlaintextAddress(host, port, config));
  }

  private ManagedChannel intercept(ManagedChannel channel) {
    return this.interceptedChannels.computeIfAbsent(
        channel, raw -> new InterceptedManagedChannel(raw, this.interceptor));
  }

  // The inherited forAddress calls forPlaintextAddress, so delegating it would return the raw
  // channel and skip the interceptor.
  private interface ForAddress {
    ManagedChannel forAddress(String host, int port);

    ManagedChannel forSecureAddress(String host, int port);

    ManagedChannel forSecureAddress(String host, int port, GrpcChannelConfig config);

    ManagedChannel forPlaintextAddress(String host, int port);

    ManagedChannel forPlaintextAddress(String host, int port, GrpcChannelConfig config);
  }

  private static final class InterceptedManagedChannel extends ManagedChannel {
    @Delegate(excludes = InterceptedCall.class)
    private final ManagedChannel delegate;

    private final Channel interceptedChannel;

    private InterceptedManagedChannel(ManagedChannel delegate, ClientInterceptor interceptor) {
      this.delegate = delegate;
      this.interceptedChannel = ClientInterceptors.intercept(delegate, interceptor);
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> newCall(
        MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions) {
      return this.interceptedChannel.newCall(methodDescriptor, callOptions);
    }

    @Override
    public String authority() {
      return this.interceptedChannel.authority();
    }

    @Override
    public ManagedChannel shutdown() {
      this.delegate.shutdown();
      return this;
    }

    @Override
    public ManagedChannel shutdownNow() {
      this.delegate.shutdownNow();
      return this;
    }
  }

  // shutdown and shutdownNow must return this wrapper, not the raw delegate channel.
  private interface InterceptedCall {
    <ReqT, RespT> ClientCall<ReqT, RespT> newCall(
        MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions);

    String authority();

    ManagedChannel shutdown();

    ManagedChannel shutdownNow();
  }
}
