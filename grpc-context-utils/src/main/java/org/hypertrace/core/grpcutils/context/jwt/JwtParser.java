package org.hypertrace.core.grpcutils.context.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.hypertrace.core.grpcutils.context.RequestContextConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JwtParser {
  private static final Logger LOG = LoggerFactory.getLogger(JwtParser.class);
  private static final String BEARER_TOKEN_PREFIX = "Bearer ";

  private final JwtVerifierFactory jwtVerifierFactory;
  private final Cache<String, Optional<VerifiedJwt>> verifiedJwtCache =
      CacheBuilder.newBuilder().maximumSize(1000).expireAfterAccess(1, TimeUnit.HOURS).build();

  public JwtParser(String expectedJwtAudience, String jwtIssuer) {
    this.jwtVerifierFactory = new JwtVerifierFactory(expectedJwtAudience, jwtIssuer);
  }

  public Optional<VerifiedJwt> fromRequestContext(RequestContext requestContext) {
    return requestContext
        .get(RequestContextConstants.AUTHORIZATION_HEADER)
        .flatMap(this::fromAuthHeader);
  }

  public Optional<VerifiedJwt> fromAuthHeader(String authHeaderValue) {
    if (authHeaderValue.startsWith(BEARER_TOKEN_PREFIX)) {
      return this.fromJwt(authHeaderValue.substring(BEARER_TOKEN_PREFIX.length()));
    }

    return Optional.empty();
  }

  public Optional<VerifiedJwt> fromJwt(String jwtValue) {
    try {
      return this.verifiedJwtCache.get(jwtValue, () -> this.decodeAndVerify(jwtValue));
    } catch (ExecutionException e) {
      LOG.warn("Exception loading verified jwt from cache", e);
      return Optional.empty();
    }
  }

  private Optional<VerifiedJwt> decodeAndVerify(String jwtString) {
    try {
      DecodedJWT jwt = JWT.decode(jwtString);
      JWTVerifier verifier = this.jwtVerifierFactory.create(jwt.getKeyId());
      verifier.verify(jwt);
      return Optional.of(new DefaultVerifiedJwt(jwt));
    } catch (Throwable t) {
      LOG.warn("Failed to verify JWT", t);
      return Optional.empty();
    }
  }

  private static final class DefaultVerifiedJwt implements VerifiedJwt {
    private final DecodedJWT jwt;
    private static final String SUBJECT_CLAIM = "sub";
    private static final String NAME_CLAIM = "name";
    private static final String PICTURE_CLAIM = "picture";
    private static final String EMAIL_CLAIM = "email";

    private DefaultVerifiedJwt(DecodedJWT jwt) {
      this.jwt = jwt;
    }

    @Override
    public String getUserId() {
      return jwt.getClaim(SUBJECT_CLAIM).asString();
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
  }
}
