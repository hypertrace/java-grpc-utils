package org.hypertrace.core.grpcutils.server;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.grpc.Deadline;
import io.grpc.Deadline.Ticker;
import io.grpc.Server;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ServerManagementUtilTest {

  @Test
  void canShutdownGracefully() throws InterruptedException {
    Server mockServer = mock(Server.class);
    when(mockServer.isTerminated()).thenReturn(true);
    Ticker mockTicker = mock(Ticker.class);
    when(mockTicker.nanoTime()).thenReturn(0L);
    ServerManagementUtil.shutdownServer(
        mockServer, "mockServer", Deadline.after(10, MILLISECONDS, mockTicker));

    InOrder serverVerifier = inOrder(mockServer);
    serverVerifier.verify(mockServer).shutdown();
    serverVerifier.verify(mockServer).awaitTermination(10, MILLISECONDS);
    serverVerifier.verify(mockServer).isTerminated();
    serverVerifier.verifyNoMoreInteractions();
  }

  @Test
  void canShutdownForcefully() throws InterruptedException {
    Server mockServer = mock(Server.class);
    when(mockServer.isTerminated()).thenReturn(false);
    Ticker mockTicker = mock(Ticker.class);
    when(mockTicker.nanoTime()).thenReturn(0L);
    ServerManagementUtil.shutdownServer(
        mockServer, "mockServer", Deadline.after(10, MILLISECONDS, mockTicker));

    InOrder serverVerifier = inOrder(mockServer);
    serverVerifier.verify(mockServer).shutdown();
    serverVerifier.verify(mockServer).awaitTermination(10, MILLISECONDS);
    serverVerifier.verify(mockServer).isTerminated();
    serverVerifier.verify(mockServer).shutdownNow();
    serverVerifier.verify(mockServer).awaitTermination(5010, MILLISECONDS);
    serverVerifier.verify(mockServer).isTerminated();
    serverVerifier.verifyNoMoreInteractions();
  }
}
