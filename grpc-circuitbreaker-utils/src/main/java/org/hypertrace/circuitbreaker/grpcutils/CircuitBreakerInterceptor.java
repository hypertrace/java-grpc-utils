package org.hypertrace.circuitbreaker.grpcutils;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;

public abstract class CircuitBreakerInterceptor implements ClientInterceptor {
  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
    if (!isCircuitBreakerEnabled()) {
      return next.newCall(method, callOptions);
    }
    return createInterceptedCall(method, callOptions, next);
  }

  protected abstract boolean isCircuitBreakerEnabled();

  protected abstract <ReqT, RespT> ClientCall<ReqT, RespT> createInterceptedCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next);
}
