package org.hypertrace.core.grpcutils.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ServerCalls;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GrpcChannelRegistryRetryTest {

  @Test
  void retriesOnUnavailableWhenRetryPolicyConfigured() throws Exception {
    String serverName = UUID.randomUUID().toString();
    AtomicInteger callCount = new AtomicInteger(0);
    MethodDescriptor<byte[], byte[]> method =
        MethodDescriptor.<byte[], byte[]>newBuilder()
            .setType(MethodType.UNARY)
            .setFullMethodName("test.Service/Call")
            .setRequestMarshaller(inputStreamMarshaller())
            .setResponseMarshaller(inputStreamMarshaller())
            .build();

    // Fail first 2 attempts with UNAVAILABLE, succeed on 3rd
    Server server =
        InProcessServerBuilder.forName(serverName)
            .addService(
                ServerServiceDefinition.builder("test.Service")
                    .addMethod(
                        method,
                        ServerCalls.asyncUnaryCall(
                            (request, responseObserver) -> {
                              if (callCount.incrementAndGet() < 3) {
                                responseObserver.onError(Status.UNAVAILABLE.asRuntimeException());
                              } else {
                                responseObserver.onNext(new byte[0]);
                                responseObserver.onCompleted();
                              }
                            }))
                    .build())
            .build()
            .start();

    GrpcServiceConfig serviceConfig =
        GrpcServiceConfig.builder()
            .retryPolicy(
                GrpcRetryPolicy.builder()
                    .maxAttempts(3)
                    .initialBackoff(Duration.ofMillis(10))
                    .maxBackoff(Duration.ofMillis(100))
                    .backoffMultiplier(2.0)
                    .retryableStatusCode(Status.Code.UNAVAILABLE)
                    .build())
            .build();

    InProcessGrpcChannelRegistry registry = new InProcessGrpcChannelRegistry();
    ManagedChannel channel =
        registry.forName(
            serverName,
            GrpcChannelConfig.builder().serviceConfig(serviceConfig).enableRetry(true).build());

    ClientCalls.blockingUnaryCall(channel, method, CallOptions.DEFAULT, new byte[0]);

    assertEquals(3, callCount.get());
    server.shutdown();
    registry.shutdown();
  }

  private static MethodDescriptor.Marshaller<byte[]> inputStreamMarshaller() {
    return new MethodDescriptor.Marshaller<>() {
      @Override
      public InputStream stream(byte[] value) {
        return new ByteArrayInputStream(value);
      }

      @Override
      public byte[] parse(InputStream stream) {
        return new byte[0];
      }
    };
  }
}
