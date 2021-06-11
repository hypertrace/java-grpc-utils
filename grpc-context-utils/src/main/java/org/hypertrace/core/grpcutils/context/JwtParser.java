package org.hypertrace.core.grpcutils.context;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JwtParser {
  private static final Logger LOG = LoggerFactory.getLogger(JwtParser.class);
  private static final String BEARER_TOKEN_PREFIX = "Bearer ";

  private final Cache<String, Optional<Jwt>> jwtCache =
      CacheBuilder.newBuilder().maximumSize(1000).expireAfterAccess(1, TimeUnit.HOURS).build();

  Optional<Jwt> fromAuthHeader(String authHeaderValue) {
    if (authHeaderValue.startsWith(BEARER_TOKEN_PREFIX)) {
      return this.fromJwt(authHeaderValue.substring(BEARER_TOKEN_PREFIX.length()));
    }
    return Optional.empty();
  }

  Optional<Jwt> fromJwt(String jwtValue) {
    try {
      return this.jwtCache.get(jwtValue, () -> this.decode(jwtValue));
    } catch (ExecutionException e) {
      LOG.warn("Exception loading jwt from cache", e);
      return Optional.empty();
    }
  }

  private Optional<Jwt> decode(String jwtString) {
    try {
      DecodedJWT jwt = JWT.decode(jwtString);
      return Optional.of(new DefaultJwt(jwt));
    } catch (Throwable t) {
      LOG.warn("Failed to verify JWT", t);
      return Optional.empty();
    }
  }

  private static final class DefaultJwt implements Jwt {
    private final DecodedJWT jwt;
    private static final String SUBJECT_CLAIM = "sub";
    private static final String NAME_CLAIM = "name";
    private static final String PICTURE_CLAIM = "picture";
    private static final String EMAIL_CLAIM = "email";
    private static final String ROLES_CLAIM = "https://traceable.ai/roles";

    private DefaultJwt(DecodedJWT jwt) {
      this.jwt = jwt;
    }

    @Override
    public Optional<String> getUserId() {
      return Optional.ofNullable(jwt.getClaim(SUBJECT_CLAIM).asString());
    }

    @Override
    public Optional<String> getName() {
      return Optional.ofNullable(jwt.getClaim(NAME_CLAIM).asString());
    }

    @Override
    public Optional<String> getPictureUrl() {
      return Optional.ofNullable(jwt.getClaim(PICTURE_CLAIM).asString());
    }

    @Override
    public Optional<String> getEmail() {
      return Optional.ofNullable(jwt.getClaim(EMAIL_CLAIM).asString());
    }

    @Override
    public List<String> getRoles() {
      List<String> roles = jwt.getClaim(ROLES_CLAIM).asList(String.class);
      if (roles == null || roles.isEmpty()) {
        return Collections.emptyList();
      }
      return roles;
    }
  }
}
