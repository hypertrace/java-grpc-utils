package org.hypertrace.core.grpcutils.server;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.grpc.Server;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ServerManagementUtilTest {

  @Test
  void canShutdownGracefully() throws InterruptedException {
    Server mockServer = mock(Server.class);
    when(mockServer.isTerminated()).thenReturn(true);
    ServerManagementUtil.shutdownServer(
        mockServer, "mockServer", Duration.of(10, ChronoUnit.MILLIS));

    InOrder serverVerifier = inOrder(mockServer);
    serverVerifier.verify(mockServer).shutdown();
    serverVerifier.verify(mockServer).awaitTermination(10, TimeUnit.MILLISECONDS);
    serverVerifier.verify(mockServer).isTerminated();
    serverVerifier.verifyNoMoreInteractions();
  }

  @Test
  void canShutdownForcefully() throws InterruptedException {
    Server mockServer = mock(Server.class);
    when(mockServer.isTerminated()).thenReturn(false);
    ServerManagementUtil.shutdownServer(
        mockServer, "mockServer", Duration.of(10, ChronoUnit.MILLIS));

    InOrder serverVerifier = inOrder(mockServer);
    serverVerifier.verify(mockServer).shutdown();
    serverVerifier.verify(mockServer).awaitTermination(10, TimeUnit.MILLISECONDS);
    serverVerifier.verify(mockServer).isTerminated();
    serverVerifier.verify(mockServer).shutdownNow();
    serverVerifier.verify(mockServer).awaitTermination(5000, TimeUnit.MILLISECONDS);
    serverVerifier.verify(mockServer).isTerminated();
    serverVerifier.verifyNoMoreInteractions();
  }
}
