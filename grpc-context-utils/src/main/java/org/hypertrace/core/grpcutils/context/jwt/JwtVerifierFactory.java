package org.hypertrace.core.grpcutils.context.jwt;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.JWTVerifier;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.TimeUnit;

public class JwtVerifierFactory {

  private final JwkProvider jwkProvider;
  private final String expectedJwtAudience;
  private final String expectedJwtIssuer;

  JwtVerifierFactory(String expectedJwtAudience, String jwtIssuer) {
    this.expectedJwtAudience = expectedJwtAudience;
    this.expectedJwtIssuer = jwtIssuer;
    this.jwkProvider =
        new JwkProviderBuilder(jwtIssuer)
            .cached(5, 1, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build();
  }

  public JWTVerifier create(String keyId) throws JwkException {
    Jwk jwk = jwkProvider.get(keyId);
    // Private key not required for verification
    Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);

    return JWT.require(algorithm)
        .withIssuer(this.expectedJwtIssuer)
        .withAudience(this.expectedJwtAudience)
        .build();
  }
}
