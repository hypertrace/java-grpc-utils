package org.hypertrace.core.grpcutils.context;

import java.util.Optional;

public interface Jwt {
  Optional<String> getUserId();

  Optional<String> getName();

  Optional<String> getPictureUrl();

  Optional<String> getEmail();
}
