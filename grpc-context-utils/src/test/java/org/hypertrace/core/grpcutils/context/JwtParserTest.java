package org.hypertrace.core.grpcutils.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class JwtParserTest {
  private final String testJwt =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE2MjEzNjM1OTcsImV4cCI6MTY1Mjg5OTU5NywiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIkdpdmVuTmFtZSI6IkpvaG5ueSIsIlN1cm5hbWUiOiJSb2NrZXQiLCJuYW1lIjoiSm9obm55IFJvY2tldCIsImVtYWlsIjoianJvY2tldEBleGFtcGxlLmNvbSIsInBpY3R1cmUiOiJ3d3cuZXhhbXBsZS5jb20iLCJyb2xlcyI6WyJzdXBlcl91c2VyIiwidXNlciIsImJpbGxpbmdfYWRtaW4iXX0.lEDjPPCjr-Epv6pNslq-HK9vmxfstp1sY85GstlbU1I";
  private final String testJwtUserId = "jrocket@example.com";
  private final String testJwtName = "Johnny Rocket";
  private final String testJwtPictureUrl = "www.example.com";
  private final String testJwtEmail = "jrocket@example.com";
  private final String testRolesClaim = "roles";
  private final List<String> testRoles = ImmutableList.of("super_user", "user", "billing_admin");

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
  void testClaimCanBeParsedFromToken() {
    JwtParser parser = new JwtParser();
    Optional<Jwt> jwt = parser.fromJwt(testJwt);
    assertEquals(
        Optional.of(testRoles),
        jwt.flatMap(j -> j.getClaim(testRolesClaim)).flatMap(claim -> claim.asList(String.class)));
  }

  @Test
  void testCanParseObjectClaim() {
    String jwtWithObjectArrayClaim =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE2MjEzNjM1OTcsImV4cCI6MTY1Mjg5OTU5NywiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIkdpdmVuTmFtZSI6IkpvaG5ueSIsIlN1cm5hbWUiOiJSb2NrZXQiLCJuYW1lIjoiSm9obm55IFJvY2tldCIsImVtYWlsIjoianJvY2tldEBleGFtcGxlLmNvbSIsInBpY3R1cmUiOiJ3d3cuZXhhbXBsZS5jb20iLCJyb2xlcyI6W3siaWQiOiJzdXBlcl91c2VyIiwidmFsdWVzIjpbXX0seyJpZCI6InVzZXIiLCJ2YWx1ZXMiOlsiZm9vIl19XX0.EJyZWwbfbCAS4NJdwURAsOewf8V6863D1ZqXGTVZigE";
    /*
      {
        "iss": "Online JWT Builder",
        "iat": 1621363597,
        "exp": 1652899597,
        "aud": "www.example.com",
        "sub": "jrocket@example.com",
        "GivenName": "Johnny",
        "Surname": "Rocket",
        "name": "Johnny Rocket",
        "email": "jrocket@example.com",
        "picture": "www.example.com",
        "roles": [
          {
            "id": "super_user",
            "values": []
          },
          {
            "id": "user",
            "values": [
              "foo"
            ]
          }
        ]
      }
    */
    JwtParser parser = new JwtParser();
    Optional<Jwt> jwt = parser.fromJwt(jwtWithObjectArrayClaim);

    assertEquals(
        Optional.of(
            List.of(
                new TestObject("super_user", List.of()), new TestObject("user", List.of("foo")))),
        jwt.flatMap(j -> j.getClaim("roles")).flatMap(claim -> claim.asList(TestObject.class)));
  }

  @Value
  @NoArgsConstructor(force = true)
  @AllArgsConstructor
  private static class TestObject {
    @JsonProperty String id;
    @JsonProperty List<String> values;
  }
}
