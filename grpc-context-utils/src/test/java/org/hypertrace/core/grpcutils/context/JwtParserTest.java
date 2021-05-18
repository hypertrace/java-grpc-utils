package org.hypertrace.core.grpcutils.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class JwtParserTest {
  private final String testJwt =
      "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IlFqYzBNell4UkRKRVJUSkVOMFZGTlVRMk4wVXlOVFZCTlVVME1rVTBSVUl6T0VZNVF6VTFPQSJ9.eyJodHRwczovL3RyYWNlYWJsZS5haS9yb2xlcyI6WyJ1c2VyIiwidHJhY2VhYmxlIl0sImh0dHBzOi8vdHJhY2VhYmxlLmFpL2N1c3RvbWVyX2lkIjoiM2U3NjE4NzktYzc3Yi00ZDhmLWEwNzUtNjJmZjI4ZThmYThhIiwiZ2l2ZW5fbmFtZSI6IkFhcm9uIiwiZmFtaWx5X25hbWUiOiJTdGVpbmZlbGQiLCJuaWNrbmFtZSI6ImFhcm9uIiwibmFtZSI6IkFhcm9uIFN0ZWluZmVsZCIsInBpY3R1cmUiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS0vQU9oMTRHamVidGRIcnJlWnlOZmNqVTd0ZjhxWThmVHFnT1o5VHdJTzFUTTkiLCJsb2NhbGUiOiJlbiIsInVwZGF0ZWRfYXQiOiIyMDIwLTA5LTMwVDE3OjMyOjQyLjUwNVoiLCJlbWFpbCI6ImFhcm9uQHRyYWNlYWJsZS5haSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJpc3MiOiJodHRwczovL2F1dGgudHJhY2VhYmxlLmFpLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8MTA4NzE0Njk2ODQ3Nzc5OTk5MzU5IiwiYXVkIjoiNUVIVUpGTDFTa29VczRWYjJ0SVZWVTk5WGZCdUNrTUkiLCJpYXQiOjE2MDE3ODY3MjgsImV4cCI6MTYwMTgyMjcyOH0.rUVnA0Q13djUm7qAdQKwe6RWJycvdg2Hj5h8IKB2D47febVn90mRr2fmlvWjAaGLHB4fosP-zjD9lriW1UnEXk0x4oPfRl5hK9lVBVhkTUk7FA-YX_R6p1ZFALt4itqXxZHdMQZt_41qXieu4uYOOqdAWJmkhDqUVBUMaHJRAdOOfPWnzfWyaOLOG39MaTFvJQA0K5D4EFZV_1z5mV4RA_xyp_qxQDj4T0m8-4IPIIFt0uilQZhL_yrm6CJwyWdoeexI6f7swC5jybzgDrbgmdxBdeh2mCu_EYLoQEP4tXtWKpvYNqAkG8XO-Vhx7a-wyWIY7SbRxi3vjGkXT8-Bbg";
  private final String testJwtUserId = "google-oauth2|108714696847779999359";
  private final String testJwtName = "Aaron Steinfeld";
  private final String testJwtPictureUrl =
      "https://lh3.googleusercontent.com/a-/AOh14GjebtdHrreZyNfcjU7tf8qY8fTqgOZ9TwIO1TM9";
  private final String testJwtEmail = "aaron@traceable.ai";

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
}
