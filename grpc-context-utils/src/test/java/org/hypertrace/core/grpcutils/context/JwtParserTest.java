package org.hypertrace.core.grpcutils.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class JwtParserTest {
  private final String testJwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE2MjEzNjM1OTcsImV4cCI6MTY1Mjg5OTU5NywiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIkdpdmVuTmFtZSI6IkpvaG5ueSIsIlN1cm5hbWUiOiJSb2NrZXQiLCJuYW1lIjoiSm9obm55IFJvY2tldCIsImVtYWlsIjoianJvY2tldEBleGFtcGxlLmNvbSIsInBpY3R1cmUiOiJ3d3cuZXhhbXBsZS5jb20iLCJodHRwczovL3RyYWNlYWJsZS5haS9yb2xlcyI6WyJ0cmFjZWFibGUiLCJ1c2VyIiwiYmlsbGluZ19hZG1pbiJdfQ.xdWar7cgJ_5V3SgECanVtBMhxJGb-DbeIfrKSpAQLJM";
  private final String emptyRolesJwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE2MjEzNjM1OTcsImV4cCI6MTY1Mjg5OTU5NywiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIkdpdmVuTmFtZSI6IkpvaG5ueSIsIlN1cm5hbWUiOiJSb2NrZXQiLCJuYW1lIjoiSm9obm55IFJvY2tldCIsImVtYWlsIjoianJvY2tldEBleGFtcGxlLmNvbSIsInBpY3R1cmUiOiJ3d3cuZXhhbXBsZS5jb20iLCJodHRwczovL3RyYWNlYWJsZS5haS9yb2xlcyI6W119.sFUMZNyypj379xy5P4kqTbBXBOR5XvX2nhpKx6YiiwU";
  private final String noRolesJwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE2MjEzNjM1OTcsImV4cCI6MTY1Mjg5OTU5NywiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIkdpdmVuTmFtZSI6IkpvaG5ueSIsIlN1cm5hbWUiOiJSb2NrZXQiLCJuYW1lIjoiSm9obm55IFJvY2tldCIsImVtYWlsIjoianJvY2tldEBleGFtcGxlLmNvbSIsInBpY3R1cmUiOiJ3d3cuZXhhbXBsZS5jb20ifQ.Ui1Z2RhiVe3tq6uJPgcyjsfDBdeOeINs_gXEHC6cdpU";
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
    assertEquals(Optional.of(testRoles), jwt.map(Jwt::getRoles));
  }

  @Test
  void testRolesAreEmptyIfRolesArrayIsEmptyInJwt() {
    JwtParser parser = new JwtParser();
    Optional<Jwt> jwt = parser.fromJwt(emptyRolesJwt);
    assertEquals(Optional.of(Collections.emptySet()), jwt.map(Jwt::getRoles));
  }

  @Test
  void testRolesAreEmptyIfRolesIfNoRolesClaimInToken() {
    JwtParser parser = new JwtParser();
    Optional<Jwt> jwt = parser.fromJwt(noRolesJwt);
    assertEquals(Optional.of(Collections.emptySet()), jwt.map(Jwt::getRoles));
  }

}
