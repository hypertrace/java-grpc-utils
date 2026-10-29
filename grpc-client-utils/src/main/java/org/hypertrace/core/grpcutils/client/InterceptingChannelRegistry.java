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
      final GrpcChannelRegistry registry, final ClientInterceptor interceptor) {
    return new InterceptingChannelRegistry(
        Objects.requireNonNull(registry), Objects.requireNonNull(interceptor));
  }

  // Lombok skips a delegate method only when this class overrides it. forAddress is not
  // overridden: the superclass implementation calls forPlaintextAddress. Delegating it would
  // return the raw channel and skip the interceptor.
  @Delegate(excludes = ForAddress.class)
  private final GrpcChannelRegistry delegate;

  private final ClientInterceptor interceptor;
  private final Map<ManagedChannel, ManagedChannel> interceptedChannels =
      Collections.synchronizedMap(new IdentityHashMap<>());

  private InterceptingChannelRegistry(
      final GrpcChannelRegistry delegate, final ClientInterceptor interceptor) {
    this.delegate = delegate;
    this.interceptor = interceptor;
  }

  @Override
  public ManagedChannel forSecureAddress(final String host, final int port) {
    return this.intercept(this.delegate.forSecureAddress(host, port));
  }

  @Override
  public ManagedChannel forSecureAddress(
      final String host, final int port, final GrpcChannelConfig config) {
    return this.intercept(this.delegate.forSecureAddress(host, port, config));
  }

  @Override
  public ManagedChannel forPlaintextAddress(final String host, final int port) {
    return this.intercept(this.delegate.forPlaintextAddress(host, port));
  }

  @Override
  public ManagedChannel forPlaintextAddress(
      final String host, final int port, final GrpcChannelConfig config) {
    return this.intercept(this.delegate.forPlaintextAddress(host, port, config));
  }

  private ManagedChannel intercept(final ManagedChannel channel) {
    return this.interceptedChannels.computeIfAbsent(
        channel, raw -> new InterceptedManagedChannel(raw, this.interceptor));
  }

  private interface ForAddress {
    ManagedChannel forAddress(String host, int port);
  }

  private static final class InterceptedManagedChannel extends ManagedChannel {
    // Overridden methods are not delegated. shutdown and shutdownNow return this wrapper.
    @Delegate private final ManagedChannel delegate;

    private final Channel interceptedChannel;

    private InterceptedManagedChannel(
        final ManagedChannel delegate, final ClientInterceptor interceptor) {
      this.delegate = delegate;
      this.interceptedChannel = ClientInterceptors.intercept(delegate, interceptor);
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> newCall(
        final MethodDescriptor<ReqT, RespT> methodDescriptor, final CallOptions callOptions) {
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
}
