package org.hypertrace.core.grpcutils.client;

import static java.util.Objects.isNull;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimeoutVerifyingClientInterceptor implements ClientInterceptor {

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
    if (isNull(callOptions.getDeadline())) {
      log.warn("Missing deadline for call to method {}", methodDescriptor.getFullMethodName());
    }

    return channel.newCall(methodDescriptor, callOptions);
  }
}
