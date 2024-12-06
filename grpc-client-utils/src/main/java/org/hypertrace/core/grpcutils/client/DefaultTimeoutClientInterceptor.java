package org.hypertrace.core.grpcutils.client;

import static java.util.Objects.isNull;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Context;
import io.grpc.MethodDescriptor;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class DefaultTimeoutClientInterceptor implements ClientInterceptor {
  private final @Nonnull Duration defaultTimeout;

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions providedOptions, Channel next) {

    if (isNull(providedOptions.getDeadline()) && isNull(Context.current().getDeadline())) {
      return next.newCall(
          methodDescriptor,
          providedOptions.withDeadlineAfter(defaultTimeout.toMillis(), TimeUnit.MILLISECONDS));
    }

    return next.newCall(methodDescriptor, providedOptions);
  }
}
