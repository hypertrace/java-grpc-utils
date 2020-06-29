package org.hypertrace.core.grpcutils.client;

import io.grpc.CallCredentials;
import java.util.function.Supplier;

public interface ClientCallCredentialsProvider extends Supplier<CallCredentials> {
  CallCredentials get();
}
