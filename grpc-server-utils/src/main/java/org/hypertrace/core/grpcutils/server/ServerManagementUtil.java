package org.hypertrace.core.grpcutils.server;

import io.grpc.Server;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerManagementUtil {

  public static void shutdownServer(Server grpcServer, String name, Duration timeout) {
    log.info("Starting shutdown for service [{}]", name);
    grpcServer.shutdown();
    boolean gracefullyShutdown = waitForGracefulShutdown(grpcServer, name, timeout);
    if (!gracefullyShutdown) {
      forceShutdown(grpcServer, name);
    }
  }

  private static boolean waitForGracefulShutdown(Server grpcServer, String name, Duration timeout) {
    boolean successfullyShutdown = waitForTermination(grpcServer, name, timeout);
    if (successfullyShutdown) {
      log.info("Shutdown service successfully [{}]", name);
    }
    return successfullyShutdown;
  }

  private static void forceShutdown(Server grpcServer, String name) {
    log.error("Shutting down service [{}] forcefully", name);
    grpcServer.shutdownNow();
    if (waitForTermination(grpcServer, name, Duration.ofSeconds(5))) {
      log.error("Forced service [{}] shutdown successful", name);
    } else {
      log.error("Unable to force service [{}] shutdown in 5s - giving up!", name);
    }
  }

  private static boolean waitForTermination(Server grpcServer, String name, Duration deadline) {
    try {
      if (!grpcServer.awaitTermination(deadline.toMillis(), TimeUnit.MILLISECONDS)) {
        log.error("Service [{}] did not shut down after waiting", name);
      }
    } catch (InterruptedException ex) {
      log.error("There has been an interruption while waiting for service [{}] to shutdown", name);
      Thread.currentThread().interrupt();
    }
    return grpcServer.isTerminated();
  }
}
