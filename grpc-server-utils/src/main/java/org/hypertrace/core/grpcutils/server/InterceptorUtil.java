package org.hypertrace.core.grpcutils.server;

import io.grpc.BindableService;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;

/** Utility class to wrap any GRPC service with all the common interceptors */
public class InterceptorUtil {

  /**
   * Wraps the service through a chain of interceptors
   *
   * @param bindableService service to be intercepted
   * @return the intercepted service definition
   */
  public static ServerServiceDefinition wrapInterceptors(BindableService bindableService) {
    return ServerInterceptors.intercept(
        bindableService, new RequestContextServerInterceptor(), new ThrowableResponseInterceptor());
  }
}
