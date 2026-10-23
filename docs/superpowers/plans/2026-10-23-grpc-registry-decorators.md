# gRPC Registry Decorators Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish a registry decorator that installs a default deadline, and a service-keyed circuit breaker that clients can install either as that decorator or as a standalone interceptor.

**Architecture:** `grpc-client-utils` owns `InterceptingChannelRegistry`, a decorator over an already-built `GrpcChannelRegistry`, and `TimeoutChannelRegistry`, which opts in through `grpc.client.timeout`. `grpc-circuitbreaker-utils` owns the service-keyed interceptor and `CircuitBreakerChannelRegistry`, which uses the same decorator. The existing request-keyed interceptor is unchanged.

**Tech Stack:** Java, Gradle, JUnit 5, Mockito, gRPC, Typesafe Config, Resilience4j 1.7.1 (implementation scope only).

## Global Constraints

- Work only in this `java-grpc-utils` worktree. Do not edit `activity-event-service`. Do not push.
- Do not change request-keyed breaker behavior. `ResilienceCircuitBreakerInterceptor` still selects its breaker in `sendMessage()` and still records every non-OK status as a failure.
- Do not change the library slow-call default of 2 seconds on `CircuitBreakerThresholds`.
- Resilience4j stays an `implementation` dependency of `grpc-circuitbreaker-utils`.
- A public method that accepts `com.typesafe.config.Config` requires that dependency to be `api`.
- Comments explain why. Do not add comments that restate what the code does.
- `forAddress` on a decorator must use the overridden plaintext methods. Delegating `forAddress` to the raw registry bypasses interception. `GrpcChannelRegistry.forAddress` already calls `forPlaintextAddress`.
- `shutdown()` and `shutdownNow()` on an intercepted `ManagedChannel` return the wrapper.
- One intercepted channel is cached per delegate channel.
- `grpc.client.timeout` absent, or `enabled = false`, returns the original registry.
- `circuit.breaker.config` absent, or `enabled = false`, returns the original registry from `wrap`. `forService` still returns an interceptor, and that interceptor passes every call through when the root flag is off or the block is absent.
- `failureStatusCodes` defaults to `DEADLINE_EXCEEDED`, `UNAVAILABLE`, and `RESOURCE_EXHAUSTED`.
- The service breaker name is `MethodDescriptor.getServiceName()`.
- An open breaker throws `Status.RESOURCE_EXHAUSTED` before `next.newCall`.
- A status absent from `failureStatusCodes` is recorded with `onSuccess`, including other non-OK statuses.
- An unknown status name fails interceptor construction.
- Quoted service names are read with `ConfigUtil.joinPath`.
- A service is protected only when the root flag is true and that service block is enabled. An omitted service `enabled` inherits true. A missing service block passes through.
- Callers compose decorators. This library does not wrap timeout and circuit breaker together.
- Every Gradle command in this worktree must include `-Pversion=0.13.23`. The linked worktree does not expose git tags to the version plugin. Without that property the BOM replaces `projects.grpcContextUtils` with external `grpc-context-utils:0.13.22`, and dependency locking fails.

---

### Task 1: InterceptingChannelRegistry

**Files:**
- Create: `grpc-client-utils/src/main/java/org/hypertrace/core/grpcutils/client/InterceptingChannelRegistry.java`
- Create: `grpc-client-utils/src/test/java/org/hypertrace/core/grpcutils/client/InterceptingChannelRegistryTest.java`

**Interfaces:**
- Consumes: `org.hypertrace.core.grpcutils.client.GrpcChannelRegistry`
- Produces: `public final class InterceptingChannelRegistry extends GrpcChannelRegistry` with `public static GrpcChannelRegistry wrap(GrpcChannelRegistry registry, ClientInterceptor interceptor)`

- [ ] **Step 1: Write the failing test**

Create `InterceptingChannelRegistryTest` with tests that fail to compile until the class exists:

- `wrapReturnsDistinctRegistry`
- `plaintextAndSecureChannelsCarryTheInterceptor`
- `repeatedLookupReturnsTheSameInterceptedChannel`
- `forAddressUsesThePlaintextOverride`
- `shutdownMethodsReturnTheWrapper`

Use Mockito to mock `GrpcChannelRegistry` and `ManagedChannel`. The interceptor records the `MethodDescriptor` it sees. `forAddress` must verify `delegate.forPlaintextAddress(host, port, config)` and `verify(delegate, never()).forAddress(anyString(), anyInt())`. `shutdown()` and `shutdownNow()` on the returned channel return that same channel instance and call the matching method on the raw channel.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew -Pversion=0.13.23 :grpc-client-utils:test --tests org.hypertrace.core.grpcutils.client.InterceptingChannelRegistryTest`

Expected: compile failure because `InterceptingChannelRegistry` does not exist.

- [ ] **Step 3: Write minimal implementation**

`InterceptingChannelRegistry` is a decorator. `@Delegate(excludes = ForAddress.class)` forwards `shutdown()`, `shutdown(Deadline)`, and `isShutdown()` to the delegate. Override all four `forPlaintextAddress` and `forSecureAddress` methods and intercept the delegate channel. Cache with `Collections.synchronizedMap(new IdentityHashMap<>())`.

`wrap` throws `NullPointerException` when `registry` or `interceptor` is null.

The inner `InterceptedManagedChannel` uses `@Delegate(excludes = InterceptedCall.class)`. `newCall` and `authority` go through `ClientInterceptors.intercept`. `shutdown` and `shutdownNow` call the delegate and return `this`.

Keep one why-comment on `ForAddress`: inherited `forAddress` calls `forPlaintextAddress`, and delegating `forAddress` would return the raw channel. Keep one why-comment on `InterceptedCall`: `shutdown` and `shutdownNow` must return this wrapper.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew -Pversion=0.13.23 :grpc-client-utils:test --tests org.hypertrace.core.grpcutils.client.InterceptingChannelRegistryTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add grpc-client-utils/src/main/java/org/hypertrace/core/grpcutils/client/InterceptingChannelRegistry.java grpc-client-utils/src/test/java/org/hypertrace/core/grpcutils/client/InterceptingChannelRegistryTest.java
git commit -m "$(cat <<'EOF'
Add a channel-registry decorator that installs one client interceptor.

EOF
)"
```

Do not push.

---

### Task 2: TimeoutChannelRegistry

**Files:**
- Modify: `grpc-client-utils/build.gradle.kts`
- Create: `grpc-client-utils/src/main/java/org/hypertrace/core/grpcutils/client/TimeoutChannelRegistry.java`
- Create: `grpc-client-utils/src/test/java/org/hypertrace/core/grpcutils/client/TimeoutChannelRegistryTest.java`

**Interfaces:**
- Consumes: `InterceptingChannelRegistry.wrap(GrpcChannelRegistry, ClientInterceptor)` and `DefaultTimeoutClientInterceptor(Duration)`
- Produces: `public final class TimeoutChannelRegistry` with `public static GrpcChannelRegistry wrap(GrpcChannelRegistry registry, Config config)`. Config path is `grpc.client.timeout`. Fields are `enabled` (boolean) and `defaultTimeout` (duration).

- [ ] **Step 1: Write the failing test**

Cover these cases:

- Missing `grpc.client.timeout` returns the same registry instance.
- `enabled = false` returns the same registry instance.
- `enabled = true` without `defaultTimeout` throws `com.typesafe.config.ConfigException`.
- `enabled = true` and `defaultTimeout = 1500ms` returns a channel whose call options contain a deadline about 1500ms out when the incoming `CallOptions` and `Context` have no deadline.
- A call that already has a deadline keeps that deadline.

Parse config with `ConfigFactory.parseString`. Use a mock delegate registry and a mock raw `ManagedChannel` whose `newCall` captures `CallOptions`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew -Pversion=0.13.23 :grpc-client-utils:test --tests org.hypertrace.core.grpcutils.client.TimeoutChannelRegistryTest`

Expected: compile failure because `TimeoutChannelRegistry` does not exist.

- [ ] **Step 3: Write minimal implementation**

Add `api(commonLibs.typesafe.config)` to `grpc-client-utils/build.gradle.kts`.

`TimeoutChannelRegistry.wrap` returns `registry` when `registry` is null, `config` is null, the path is absent, or `enabled` is missing or false. When enabled, require `defaultTimeout` via `config.getDuration` on the timeout block and return `InterceptingChannelRegistry.wrap(registry, new DefaultTimeoutClientInterceptor(timeout))`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew -Pversion=0.13.23 :grpc-client-utils:test --tests org.hypertrace.core.grpcutils.client.TimeoutChannelRegistryTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add grpc-client-utils/build.gradle.kts grpc-client-utils/src/main/java/org/hypertrace/core/grpcutils/client/TimeoutChannelRegistry.java grpc-client-utils/src/test/java/org/hypertrace/core/grpcutils/client/TimeoutChannelRegistryTest.java
git commit -m "$(cat <<'EOF'
Apply a configured default deadline to every channel from a registry.

EOF
)"
```

Do not push.

---

### Task 3: Configurable failure status codes

**Files:**
- Modify: `grpc-circuitbreaker-utils/build.gradle.kts` (`implementation(commonLibs.typesafe.config)` becomes `api(commonLibs.typesafe.config)`)
- Modify: `grpc-circuitbreaker-utils/src/main/java/org/hypertrace/circuitbreaker/grpcutils/CircuitBreakerThresholds.java`
- Modify: `grpc-circuitbreaker-utils/src/main/java/org/hypertrace/circuitbreaker/grpcutils/CircuitBreakerConfigParser.java`
- Create: `grpc-circuitbreaker-utils/src/test/java/org/hypertrace/circuitbreaker/grpcutils/CircuitBreakerConfigParserTest.java`

**Interfaces:**
- Consumes: existing `CircuitBreakerConfigParser.parseConfig(Config)`
- Produces: `CircuitBreakerThresholds.failureStatusCodes`, a `Set<io.grpc.Status.Code>` whose builder default is `DEADLINE_EXCEEDED`, `UNAVAILABLE`, and `RESOURCE_EXHAUSTED`. `parseConfig` reads `failureStatusCodes` inside a threshold block. An unknown name throws `com.typesafe.config.ConfigException.BadValue`.

- [ ] **Step 1: Write the failing test**

`CircuitBreakerConfigParserTest` asserts:

- A config with only `enabled = true` yields default thresholds whose `failureStatusCodes` are exactly those three codes and whose `slowCallDurationThreshold` is `Duration.ofSeconds(2)`.
- `defaultThresholds { failureStatusCodes = [UNAVAILABLE] }` replaces the set with only `UNAVAILABLE`.
- `defaultThresholds { failureStatusCodes = [NOT_A_STATUS] }` throws `ConfigException.BadValue`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew -Pversion=0.13.23 :grpc-circuitbreaker-utils:test --tests org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerConfigParserTest`

Expected: compile failure because `getFailureStatusCodes` does not exist.

- [ ] **Step 3: Write minimal implementation**

Add the field and parse `failureStatusCodes` with `config.getStringList` and `Status.Code.valueOf`. Catch `IllegalArgumentException` and throw `new ConfigException.BadValue("failureStatusCodes", name)`. Leave `ResilienceCircuitBreakerConfigConverter` unchanged so the request-keyed interceptor still records every non-OK status.

Change the Typesafe dependency from `implementation` to `api`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew -Pversion=0.13.23 :grpc-circuitbreaker-utils:test`

Expected: the new test and the existing resilience tests pass.

- [ ] **Step 5: Commit**

```bash
git add grpc-circuitbreaker-utils/build.gradle.kts grpc-circuitbreaker-utils/src/main/java/org/hypertrace/circuitbreaker/grpcutils/CircuitBreakerThresholds.java grpc-circuitbreaker-utils/src/main/java/org/hypertrace/circuitbreaker/grpcutils/CircuitBreakerConfigParser.java grpc-circuitbreaker-utils/src/test/java/org/hypertrace/circuitbreaker/grpcutils/CircuitBreakerConfigParserTest.java
git commit -m "$(cat <<'EOF'
Parse the gRPC statuses that count as circuit-breaker failures.

EOF
)"
```

Do not push.

---

### Task 4: Service-keyed circuit breaker interceptor

**Files:**
- Create: `grpc-circuitbreaker-utils/src/main/java/org/hypertrace/circuitbreaker/grpcutils/ServiceCircuitBreakerInterceptor.java`
- Modify: `grpc-circuitbreaker-utils/src/main/java/org/hypertrace/circuitbreaker/grpcutils/CircuitBreakerInterceptorFactory.java`
- Create: `grpc-circuitbreaker-utils/src/test/java/org/hypertrace/circuitbreaker/grpcutils/ServiceCircuitBreakerInterceptorTest.java`

**Interfaces:**
- Consumes: `CircuitBreakerThresholds`, `CircuitBreakerConfigParser.parseConfig`, `CircuitBreakerConfigParser.ENABLED`, `CircuitBreakerConfigParser.DEFAULT_THRESHOLDS`
- Produces: `public static ClientInterceptor CircuitBreakerInterceptorFactory.forService(Config rootConfig)`. The breaker key is `method.getServiceName()`. Config block is `circuit.breaker.config`.

- [ ] **Step 1: Write the failing test**

Use an in-process or mocked `Channel`. Drive calls through `interceptCall`. Assert:

- Absent config and `enabled = false` call the delegate.
- Enabled config with `"ai.traceable.platform.insights.api.v1.InsightsService" { }` calls the delegate for that service. A second service name calls the delegate and does not share failures.
- After enough `UNAVAILABLE` completions to open the breaker (`minimumNumberOfCalls = 1`, `failureRateThreshold = 1`, `slidingWindowSize = 1`, `slidingWindowType = COUNT_BASED`, `waitDurationInOpenState = 60s`), the next call throws `StatusRuntimeException` with `RESOURCE_EXHAUSTED` and the delegate is not called.
- A status absent from `failureStatusCodes`, such as `INTERNAL`, does not open that breaker. Use `minimumNumberOfCalls = 1` and only `INTERNAL` responses.
- `failureStatusCodes = [NOT_A_STATUS]` throws `ConfigException.BadValue` from `forService`.
- An omitted service `enabled` inherits true. `enabled = false` on that service passes through and records nothing.

Set `slowCallDurationThreshold = 1h` in these tests so latency cannot open the breaker.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew -Pversion=0.13.23 :grpc-circuitbreaker-utils:test --tests org.hypertrace.circuitbreaker.grpcutils.ServiceCircuitBreakerInterceptorTest`

Expected: compile failure because `forService` does not exist.

- [ ] **Step 3: Write minimal implementation**

`forService` delegates to `ServiceCircuitBreakerInterceptor.create(rootConfig)`.

Parse root key `circuit.breaker.config`. When the block is absent, or `enabled` is absent or false, the interceptor calls `next.newCall` immediately.

For each other root key, read `config.getConfig(ConfigUtil.joinPath(serviceName)).withFallback(defaultThresholds)`. Build default thresholds from `defaultThresholds` with a fallback to `CircuitBreakerThresholds.builder().build()` expressed as config, or by copying builder values. A service whose resolved `enabled` is false is omitted. Store `Map<String, CircuitBreakerConfig>` keyed by the original service name. `recordException` returns true only for that service's `failureStatusCodes`.

On each call, `circuitBreakerConfigs.get(serviceName)` missing means pass through. Otherwise `CircuitBreakerRegistry.circuitBreaker(serviceName, config)`, then `tryAcquirePermission`. Rejection throws `Status.RESOURCE_EXHAUSTED.withDescription(...)`. If `next.newCall` throws, call `releasePermission` and rethrow.

The listener records elapsed nanos. `status.isOk()` and statuses outside `failureStatusCodes` call `onSuccess`. Configured failure statuses call `onError` with `status.asRuntimeException()`.

Keep the why-comment that `getConfig(key)` treats dots as path separators, so service names are read with `joinPath`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew -Pversion=0.13.23 :grpc-circuitbreaker-utils:test --tests org.hypertrace.circuitbreaker.grpcutils.ServiceCircuitBreakerInterceptorTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add grpc-circuitbreaker-utils/src/main/java/org/hypertrace/circuitbreaker/grpcutils/ServiceCircuitBreakerInterceptor.java grpc-circuitbreaker-utils/src/main/java/org/hypertrace/circuitbreaker/grpcutils/CircuitBreakerInterceptorFactory.java grpc-circuitbreaker-utils/src/test/java/org/hypertrace/circuitbreaker/grpcutils/ServiceCircuitBreakerInterceptorTest.java
git commit -m "$(cat <<'EOF'
Add a service-keyed gRPC circuit breaker interceptor.

EOF
)"
```

Do not push.

---

### Task 5: CircuitBreakerChannelRegistry

**Files:**
- Modify: `grpc-circuitbreaker-utils/build.gradle.kts` (add `api(projects.grpcClientUtils)`)
- Create: `grpc-circuitbreaker-utils/src/main/java/org/hypertrace/circuitbreaker/grpcutils/CircuitBreakerChannelRegistry.java`
- Create: `grpc-circuitbreaker-utils/src/test/java/org/hypertrace/circuitbreaker/grpcutils/CircuitBreakerChannelRegistryTest.java`

**Interfaces:**
- Consumes: `InterceptingChannelRegistry.wrap(GrpcChannelRegistry, ClientInterceptor)` and `CircuitBreakerInterceptorFactory.forService(Config)`
- Produces: `public final class CircuitBreakerChannelRegistry` with `public static GrpcChannelRegistry wrap(GrpcChannelRegistry registry, Config config)`

- [ ] **Step 1: Write the failing test**

Assert:

- Null config, empty config, and `circuit.breaker.config { enabled = false }` return the same registry instance.
- `enabled = true` with a quoted Insights service returns a different registry. A call to that service with `minimumNumberOfCalls = 1` and one `UNAVAILABLE` completion makes the following call throw `RESOURCE_EXHAUSTED` without another delegate `newCall`.
- A second plaintext lookup of the same delegate channel returns the same intercepted channel.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew -Pversion=0.13.23 :grpc-circuitbreaker-utils:test --tests org.hypertrace.circuitbreaker.grpcutils.CircuitBreakerChannelRegistryTest`

Expected: compile failure because `CircuitBreakerChannelRegistry` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
public final class CircuitBreakerChannelRegistry {
  static final String CIRCUIT_BREAKER_CONFIG = "circuit.breaker.config";

  private CircuitBreakerChannelRegistry() {}

  public static GrpcChannelRegistry wrap(GrpcChannelRegistry registry, Config config) {
    if (registry == null || config == null || !config.hasPath(CIRCUIT_BREAKER_CONFIG)) {
      return registry;
    }
    Config circuitBreakerConfig = config.getConfig(CIRCUIT_BREAKER_CONFIG);
    if (!circuitBreakerConfig.hasPath("enabled") || !circuitBreakerConfig.getBoolean("enabled")) {
      return registry;
    }
    return InterceptingChannelRegistry.wrap(
        registry, CircuitBreakerInterceptorFactory.forService(config));
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew -Pversion=0.13.23 :grpc-client-utils:test :grpc-circuitbreaker-utils:test`

Expected: PASS for both modules.

- [ ] **Step 5: Commit**

```bash
git add grpc-circuitbreaker-utils/build.gradle.kts grpc-circuitbreaker-utils/src/main/java/org/hypertrace/circuitbreaker/grpcutils/CircuitBreakerChannelRegistry.java grpc-circuitbreaker-utils/src/test/java/org/hypertrace/circuitbreaker/grpcutils/CircuitBreakerChannelRegistryTest.java
git commit -m "$(cat <<'EOF'
Install the service-keyed circuit breaker from an existing channel registry.

EOF
)"
```

Do not push.
