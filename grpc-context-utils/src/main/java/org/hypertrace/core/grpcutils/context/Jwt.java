package org.hypertrace.core.grpcutils.context;

import java.util.Optional;
import java.util.Set;

interface Jwt {
  Optional<String> getUserId();

  Optional<String> getName();

  Optional<String> getPictureUrl();

  Optional<String> getEmail();

  Set<String> getRoles();
}
