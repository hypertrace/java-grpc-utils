package org.hypertrace.core.grpcutils.context;

import com.auth0.jwt.exceptions.JWTDecodeException;
import java.util.List;
import java.util.Optional;

public interface JwtClaim {
  /**
   * Get this Claim as a List of type T
   *
   * @param <T> type
   * @param tClazz the type class
   * @return An Optional containing the converted list if conversion succeeds
   */
  <T> Optional<List<T>> asList(Class<T> tClazz);

  /**
   * Get this Claim as a custom type T.
   *
   * @param <T> type
   * @param tClazz the type class
   * @return An Optional containing the converted list if conversion succeeds
   */
  <T> Optional<T> as(Class<T> tClazz) throws JWTDecodeException;
}
