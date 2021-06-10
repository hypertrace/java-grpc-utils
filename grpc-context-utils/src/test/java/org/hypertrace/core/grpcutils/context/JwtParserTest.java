package org.hypertrace.core.grpcutils.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class JwtParserTest {
  private final String testJwt =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE2MjEzNjM1OTcsImV4cCI6MTY1Mjg5OTU5NywiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIkdpdmVuTmFtZSI6IkpvaG5ueSIsIlN1cm5hbWUiOiJSb2NrZXQiLCJuYW1lIjoiSm9obm55IFJvY2tldCIsImVtYWlsIjoianJvY2tldEBleGFtcGxlLmNvbSIsInBpY3R1cmUiOiJ3d3cuZXhhbXBsZS5jb20iLCJSb2xlIjpbIk1hbmFnZXIiLCJQcm9qZWN0IEFkbWluaXN0cmF0b3IiXX0.aesOuNIamZkTMR30CBt0J9NMZZt9iLRETa5ayN_EcVs";
  private final String testJwtUserId = "jrocket@example.com";
  private final String testJwtName = "Johnny Rocket";
  private final String testJwtPictureUrl = "www.example.com";
  private final String testJwtEmail = "jrocket@example.com";
  private final Set<String> testRoles = ImmutableSet.of("traceable", "user", "billing_admin");

  @Test
  void testGoodJwtParse() {
    JwtParser parser = new JwtParser();
    Optional<Jwt> jwt = parser.fromJwt(testJwt);
    assertEquals(Optional.of(testJwtUserId), jwt.flatMap(Jwt::getUserId));
    assertEquals(Optional.of(testJwtName), jwt.flatMap(Jwt::getName));
    assertEquals(Optional.of(testJwtPictureUrl), jwt.flatMap(Jwt::getPictureUrl));
    assertEquals(Optional.of(testJwtEmail), jwt.flatMap(Jwt::getEmail));
  }

  @Test
  void testBadJwtParse() {
    JwtParser parser = new JwtParser();
    Optional<Jwt> jwt = parser.fromJwt("fake jwt");
    assertTrue(jwt.isEmpty());
  }

  @Test
  void testExtractBearerTokenPassesThrough() {
    JwtParser parser = mock(JwtParser.class);
    when(parser.fromAuthHeader(anyString())).thenCallRealMethod();

    parser.fromAuthHeader("Bearer foobar");
    verify(parser).fromJwt("foobar");
  }

  @Test
  void testExtractBearerTokenReturnsEmptyOnMalformed() {
    JwtParser parser = mock(JwtParser.class);
    when(parser.fromAuthHeader(anyString())).thenCallRealMethod();

    assertEquals(Optional.empty(), parser.fromAuthHeader("Bad header"));
    verify(parser, times(0)).fromJwt(ArgumentMatchers.any());
  }

  @Test
  void testTraceableRolesCanBeParsedFromToken() {
    JwtParser parser = new JwtParser();
    Optional<Jwt> jwt = parser.fromJwt(testJwt);
    assertEquals(Optional.of(testRoles), jwt.flatMap(Jwt::getEmail));
  }
}
