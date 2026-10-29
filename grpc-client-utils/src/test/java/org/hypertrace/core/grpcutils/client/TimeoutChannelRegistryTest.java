package org.hypertrace.core.grpcutils.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.Deadline;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.Marshaller;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimeoutChannelRegistryTest {

  private static final MethodDescriptor<String, String> METHOD =
      MethodDescriptor.<String, String>newBuilder()
          .setType(MethodDescriptor.MethodType.UNARY)
          .setFullMethodName("test.Service/Method")
          .setRequestMarshaller(new StringMarshaller())
          .setResponseMarshaller(new StringMarshaller())
          .build();

  private final AtomicReference<CallOptions> capturedOptions = new AtomicReference<>();

  private GrpcChannelRegistry delegate;
  private ManagedChannel rawChannel;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void beforeEach() {
    this.delegate = mock(GrpcChannelRegistry.class);
    this.rawChannel = mock(ManagedChannel.class);
    when(this.delegate.forPlaintextAddress("host", 1)).thenReturn(this.rawChannel);
    when(this.rawChannel.newCall(any(), any()))
        .thenAnswer(
            invocation -> {
              this.capturedOptions.set(invocation.getArgument(1));
              return mock(ClientCall.class);
            });
  }

  @Test
  void missingTimeoutBlockReturnsTheSameRegistry() {
    Config config = ConfigFactory.parseString("grpc.client.other = 1");

    assertSame(this.delegate, TimeoutChannelRegistry.wrap(this.delegate, config));
  }

  @Test
  void disabledReturnsTheSameRegistry() {
    Config config =
        ConfigFactory.parseString(
            "grpc.client.timeout { enabled = false, defaultTimeout = 1500ms }");

    assertSame(this.delegate, TimeoutChannelRegistry.wrap(this.delegate, config));
  }

  @Test
  void missingEnabledReturnsTheSameRegistry() {
    Config config = ConfigFactory.parseString("grpc.client.timeout { defaultTimeout = 1500ms }");

    assertSame(this.delegate, TimeoutChannelRegistry.wrap(this.delegate, config));
  }

  @Test
  void nullRegistryOrConfigReturnsTheInput() {
    Config config =
        ConfigFactory.parseString("grpc.client.timeout { enabled = true, defaultTimeout = 1s }");

    assertSame(this.delegate, TimeoutChannelRegistry.wrap(this.delegate, null));
    assertEquals(null, TimeoutChannelRegistry.wrap(null, config));
  }

  @Test
  void enabledWithoutDefaultTimeoutThrows() {
    Config config = ConfigFactory.parseString("grpc.client.timeout { enabled = true }");

    assertThrows(ConfigException.class, () -> TimeoutChannelRegistry.wrap(this.delegate, config));
  }

  @Test
  void enabledAppliesTheDefaultDeadlineWhenNoneIsPresent() {
    GrpcChannelRegistry wrapped = TimeoutChannelRegistry.wrap(this.delegate, enabledConfig());
    assertNotSame(this.delegate, wrapped);

    wrapped.forPlaintextAddress("host", 1).newCall(METHOD, CallOptions.DEFAULT);

    Deadline deadline = this.capturedOptions.get().getDeadline();
    assertNotNull(deadline);
    long remainingMillis = deadline.timeRemaining(TimeUnit.MILLISECONDS);
    assertTrue(remainingMillis > 0 && remainingMillis <= 1500, "remaining: " + remainingMillis);
  }

  @Test
  void existingDeadlineIsKept() {
    GrpcChannelRegistry wrapped = TimeoutChannelRegistry.wrap(this.delegate, enabledConfig());
    Deadline existing = Deadline.after(10, TimeUnit.MINUTES);

    wrapped
        .forPlaintextAddress("host", 1)
        .newCall(METHOD, CallOptions.DEFAULT.withDeadline(existing));

    assertSame(existing, this.capturedOptions.get().getDeadline());
  }

  private static Config enabledConfig() {
    return ConfigFactory.parseString(
        "grpc.client.timeout { enabled = true, defaultTimeout = 1500ms }");
  }

  private static final class StringMarshaller implements Marshaller<String> {
    @Override
    public InputStream stream(String value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String parse(InputStream stream) {
      throw new UnsupportedOperationException();
    }
  }
}
