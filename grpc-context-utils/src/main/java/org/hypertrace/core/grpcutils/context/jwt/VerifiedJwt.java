package org.hypertrace.core.grpcutils.context.jwt;

import java.util.Optional;

public interface VerifiedJwt {
  String getUserId();

  Optional<String> getName();

  Optional<String> getPictureUrl();

  Optional<String> getEmail();
}
