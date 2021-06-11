package org.hypertrace.core.grpcutils.context;

import java.util.List;
import java.util.Optional;

interface Jwt {
  Optional<String> getUserId();

  Optional<String> getName();

  Optional<String> getPictureUrl();

  Optional<String> getEmail();

  List<String> getRoles();
}
