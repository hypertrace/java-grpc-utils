package org.hypertrace.core.grpcutils.server;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import io.grpc.Deadline;
import io.grpc.Server;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerManagementUtil {

  /**
   * Attempts to gracefully shutdown the server by the provided deadline. If unsuccessful, it then
   * uses up to 5 additional seconds to forcefully shutdown the server, returning with the final
   * success status.
   *
   * @param grpcServer
   * @param name
   * @param deadline
   * @return boolean - true if the server is terminated successfully, false otherwise
   */
  public static boolean shutdownServer(Server grpcServer, String name, Deadline deadline) {
    log.info("Starting shutdown for service [{}]", name);
    grpcServer.shutdown();
    return waitForGracefulShutdown(grpcServer, name, deadline)
        || forceShutdown(grpcServer, name, deadline.offset(5, SECONDS));
  }

  private static boolean waitForGracefulShutdown(
      Server grpcServer, String name, Deadline deadline) {
    boolean successfullyShutdown = waitForTermination(grpcServer, name, deadline);
    if (successfullyShutdown) {
      log.info("Shutdown service successfully [{}]", name);
    }
    return successfullyShutdown;
  }

  private static boolean forceShutdown(Server grpcServer, String name, Deadline deadline) {
    log.error("Shutting down service [{}] forcefully", name);
    grpcServer.shutdownNow();
    boolean successfullyShutdown = waitForTermination(grpcServer, name, deadline);
    if (successfullyShutdown) {
      log.error("Forced service [{}] shutdown successful", name);
    } else {
      log.error("Unable to force service [{}] shutdown in 5s - giving up!", name);
    }
    return successfullyShutdown;
  }

  private static boolean waitForTermination(Server grpcServer, String name, Deadline deadline) {
    try {
      if (!grpcServer.awaitTermination(deadline.timeRemaining(MILLISECONDS), MILLISECONDS)) {
        log.error("Service [{}] did not shut down after waiting", name);
      }
    } catch (InterruptedException ex) {
      log.error("There has been an interruption while waiting for service [{}] to shutdown", name);
      Thread.currentThread().interrupt();
    }
    return grpcServer.isTerminated();
  }
}
