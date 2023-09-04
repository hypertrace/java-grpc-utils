package org.hypertrace.core.grpcutils.context;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class UuidGenerator {
  /**
   * This function generates UUIDs using ThreadLocalRandom, which is faster and doesn't block like
   * the default randomUUID method that relies on /dev/random. It's suitable for most random UUID
   * needs.
   */
  public static UUID generateFastRandomUUID() {
    long mostSigBits = ThreadLocalRandom.current().nextLong();
    long leastSigBits = ThreadLocalRandom.current().nextLong();

    // Set the version (4) For random UUID
    mostSigBits &= 0xFFFFFFFFFFFF0FFFL;
    mostSigBits |= 0x0000000000004000L;
    // Set variant to RFC 4122
    leastSigBits &= 0x3FFFFFFFFFFFFFFFL;
    leastSigBits |= 0x8000000000000000L;

    return new UUID(mostSigBits, leastSigBits);
  }
}
