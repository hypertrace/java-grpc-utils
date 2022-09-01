package org.hypertrace.core.grpcutils.client;

import io.grpc.CallCredentials;
import org.hypertrace.core.grpcutils.context.RequestContext;

public class RequestContextClientCallCredsProviderFactory {
  private static class DefaultRequestContextClientCallCredsProvider
      implements ClientCallCredentialsProvider {
    private final ContextKeyBasedCreds credsProvider;

    public DefaultRequestContextClientCallCredsProvider() {
      this.credsProvider = new ContextKeyBasedCreds(RequestContext.CURRENT);
    }

    @Override
    public CallCredentials get() {
      return credsProvider;
    }
  }

  public static ClientCallCredentialsProvider getClientCallCredsProvider() {
    return new DefaultRequestContextClientCallCredsProvider();
  }
}
